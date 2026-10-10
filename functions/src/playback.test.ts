import * as admin from 'firebase-admin';
import { authorizePlaybackHandler } from './playback';
import 'jest';

jest.mock('@aws-sdk/cloudfront-signer', () => ({
    getSignedCookies: jest.fn().mockReturnValue({
        "CloudFront-Policy": "policy123",
        "CloudFront-Signature": "sig123",
        "CloudFront-Key-Pair-Id": "key123"
    })
}));

// Mock firebase-admin
jest.mock('firebase-admin', () => {
    const mockContentGet = jest.fn();
    const mockUserGet = jest.fn();
    const mockSubGet = jest.fn();
    const mockRateLimitTransaction = jest.fn().mockImplementation(async (cb) => {
        // Simulate empty rate-limit doc (under limit)
        return cb({
            get: jest.fn().mockResolvedValue({ exists: false, data: () => ({ calls: [] }) }),
            set: jest.fn()
        });
    });

    return {
        firestore: Object.assign(jest.fn().mockReturnValue({
            collection: jest.fn().mockImplementation((col: string) => {
                if (col === 'content') {
                    return { doc: jest.fn().mockReturnValue({ get: mockContentGet }) };
                }
                if (col === 'users') {
                    return { doc: jest.fn().mockReturnValue({ get: mockUserGet }) };
                }
                if (col === 'subscriptions') {
                    return {
                        where: jest.fn().mockReturnValue({
                            where: jest.fn().mockReturnValue({
                                where: jest.fn().mockReturnValue({
                                    get: mockSubGet
                                })
                            })
                        })
                    };
                }
                if (col === '_rateLimits') {
                    return { doc: jest.fn().mockReturnValue({}) };
                }
                return {};
            }),
            runTransaction: mockRateLimitTransaction,
        }), {
            Timestamp: {
                now: jest.fn().mockReturnValue({ toMillis: () => 1000 })
            }
        })
    };
});

describe('authorizePlaybackHandler', () => {
    let mockContentGet: jest.Mock;
    let mockUserGet: jest.Mock;
    let mockSubGet: jest.Mock;

    beforeEach(() => {
        jest.clearAllMocks();
        const db = admin.firestore();

        // Re-bind mocks after clearAllMocks
        mockContentGet = db.collection('content').doc().get as jest.Mock;
        mockUserGet = db.collection('users').doc().get as jest.Mock;
        mockSubGet = db.collection('subscriptions')
            .where('tenantId', '==', 'tenant1').where('userId', '==', 'user1').where('status', '==', 'ACTIVE').get as jest.Mock;

        // Default user mock — same tenant as content
        mockUserGet.mockResolvedValue({
            exists: true,
            data: () => ({ tenantId: 'tenant1' })
        });

        // Default rate-limit mock — allow all requests
        (admin.firestore().runTransaction as jest.Mock).mockImplementation(async (cb: Function) => {
            return cb({
                get: jest.fn().mockResolvedValue({ exists: false }),
                set: jest.fn()
            });
        });
    });

    // ── Authentication ────────────────────────────────────────────────────────

    it('rejects unauthenticated users', async () => {
        const req = { contentId: '123' };
        const context = { auth: undefined };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('UNAUTHENTICATED');
    });

    it('rejects missing contentId', async () => {
        const req = { data: {} };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow();
    });

    it('rejects contentId containing path traversal characters', async () => {
        const req = { contentId: '../other-content' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('Invalid contentId format');
    });

    it('rejects contentId containing forward slashes', async () => {
        const req = { contentId: 'tenant1/content1' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('Invalid contentId format');
    });

    it('rejects rate-limited users', async () => {
        (admin.firestore().runTransaction as jest.Mock).mockImplementation(async (cb: Function) => {
            return cb({
                get: jest.fn().mockResolvedValue({ 
                    exists: true, 
                    data: () => ({ calls: new Array(20).fill(Date.now()) }) 
                }),
                set: jest.fn()
            });
        });
        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('Too many authorization requests. Please try again later.');
    });

    // ── Content Validation ────────────────────────────────────────────────────

    it('rejects missing content', async () => {
        mockContentGet.mockResolvedValueOnce({ exists: false });
        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('CONTENT_NOT_FOUND');
    });

    it('rejects inactive (DRAFT) content', async () => {
        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({ status: 'DRAFT', tenantId: 'tenant1' })
        });
        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('CONTENT_NOT_AVAILABLE');
    });

    it('rejects ARCHIVED content', async () => {
        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({ status: 'ARCHIVED', tenantId: 'tenant1' })
        });
        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('CONTENT_NOT_AVAILABLE');
    });

    // ── Tenant Isolation ──────────────────────────────────────────────────────

    it('rejects cross-tenant access for main content', async () => {
        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({ status: 'PUBLISHED', tenantId: 'tenant1', mainPlaybackAsset: { hlsPath: 'content/tenant1/123/main/hls/master.m3u8' } })
        });
        // User belongs to tenant2
        mockUserGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({ tenantId: 'tenant2' })
        });
        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('CROSS_TENANT_ACCESS_DENIED');
    });

    it('rejects cross-tenant access for trailer', async () => {
        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({ status: 'PUBLISHED', tenantId: 'tenant1', trailerPlaybackAsset: { hlsPath: 'content/tenant1/123/trailer/hls/trailer.m3u8' } })
        });
        mockUserGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({ tenantId: 'tenant2' })
        });
        const req = { contentId: '123', isTrailer: true };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('CROSS_TENANT_ACCESS_DENIED');
    });

    // ── Subscription Enforcement ──────────────────────────────────────────────

    it('rejects main content without subscription', async () => {
        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({ status: 'PUBLISHED', tenantId: 'tenant1', mainPlaybackAsset: { hlsPath: 'content/tenant1/123/main/hls/master.m3u8' } })
        });
        mockSubGet.mockResolvedValueOnce({ empty: true });
        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('SUBSCRIPTION_REQUIRED');
    });

    it('rejects main content with expired subscription', async () => {
        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({ status: 'PUBLISHED', tenantId: 'tenant1', mainPlaybackAsset: { hlsPath: 'content/tenant1/123/main/hls/master.m3u8' } })
        });
        mockSubGet.mockResolvedValueOnce({
            empty: false,
            docs: [{ data: () => ({ expiryDate: { toMillis: () => 500 } }) }] // in the past (now=1000)
        });
        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        // Must return SUBSCRIPTION_REQUIRED — not a distinguishable EXPIRED code
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('SUBSCRIPTION_REQUIRED');
    });

    // ── Infrastructure Check ──────────────────────────────────────────────────

    it('throws PLAYBACK_NOT_CONFIGURED when CloudFront env vars missing', async () => {
        delete process.env.CLOUDFRONT_KEY_PAIR_ID;
        delete process.env.CLOUDFRONT_PRIVATE_KEY;
        delete process.env.CLOUDFRONT_DOMAIN;

        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({ status: 'PUBLISHED', tenantId: 'tenant1', mainPlaybackAsset: { hlsPath: 'content/tenant1/123/main/hls/master.m3u8' } })
        });
        mockSubGet.mockResolvedValueOnce({
            empty: false,
            docs: [{ data: () => ({ expiryDate: { toMillis: () => 2000 } }) }]
        });

        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('PLAYBACK_NOT_CONFIGURED');
    });

    // ── Successful Authorization ──────────────────────────────────────────────

    it('authorizes main content playback for valid subscriber', async () => {
        process.env.CLOUDFRONT_KEY_PAIR_ID = 'key123';
        process.env.CLOUDFRONT_PRIVATE_KEY = 'private123';
        process.env.CLOUDFRONT_DOMAIN = 'd12345.cloudfront.net';

        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({
                status: 'PUBLISHED',
                tenantId: 'tenant1',
                mainPlaybackAsset: { hlsPath: 'content/tenant1/123/main/hls/master.m3u8' }
            })
        });
        mockSubGet.mockResolvedValueOnce({
            empty: false,
            docs: [{ data: () => ({ expiryDate: { toMillis: () => 2000 } }) }]
        });

        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        const result = await authorizePlaybackHandler(req as any, context as any) as any;

        expect(result.authorized).toBe(true);
        expect(result.playbackUrl).toBe('https://d12345.cloudfront.net/content/tenant1/123/main/hls/master.m3u8');
        expect(result.cookies['CloudFront-Policy']).toBe('policy123');
        expect(result.cookies['CloudFront-Signature']).toBe('sig123');
        expect(result.cookies['CloudFront-Key-Pair-Id']).toBe('key123');
        // Verify the signed cookie resource scope is narrowly scoped to this content item
        const { getSignedCookies } = require('@aws-sdk/cloudfront-signer');
        expect(getSignedCookies).toHaveBeenCalledWith(expect.objectContaining({
            url: 'https://d12345.cloudfront.net/content/tenant1/123/main/hls/*'
        }));
    });

    // ── Path Validation ───────────────────────────────────────────────────────

    it('rejects HLS path that does not match content/tenant/id/main/hls/ prefix', async () => {
        process.env.CLOUDFRONT_KEY_PAIR_ID = 'key123';
        process.env.CLOUDFRONT_PRIVATE_KEY = 'private123';
        process.env.CLOUDFRONT_DOMAIN = 'd12345.cloudfront.net';

        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({
                status: 'PUBLISHED',
                tenantId: 'tenant1',
                mainPlaybackAsset: { hlsPath: 'content/tenant2/456/main/hls/master.m3u8' }
            })
        });
        mockSubGet.mockResolvedValueOnce({
            empty: false,
            docs: [{ data: () => ({ expiryDate: { toMillis: () => 2000 } }) }]
        });

        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('INVALID_ASSET_PATH');
    });

    it('rejects path traversal (../) stored in Firestore hlsPath', async () => {
        process.env.CLOUDFRONT_KEY_PAIR_ID = 'key123';
        process.env.CLOUDFRONT_PRIVATE_KEY = 'private123';
        process.env.CLOUDFRONT_DOMAIN = 'd12345.cloudfront.net';

        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({
                status: 'PUBLISHED',
                tenantId: 'tenant1',
                mainPlaybackAsset: { hlsPath: 'content/tenant1/123/main/hls/../../trailer/hls/master.m3u8' }
            })
        });
        mockSubGet.mockResolvedValueOnce({
            empty: false,
            docs: [{ data: () => ({ expiryDate: { toMillis: () => 2000 } }) }]
        });

        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('INVALID_ASSET_PATH');
    });

    it('rejects trailer hlsPath when main content is requested', async () => {
        process.env.CLOUDFRONT_KEY_PAIR_ID = 'key123';
        process.env.CLOUDFRONT_PRIVATE_KEY = 'private123';
        process.env.CLOUDFRONT_DOMAIN = 'd12345.cloudfront.net';

        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({
                status: 'PUBLISHED',
                tenantId: 'tenant1',
                mainPlaybackAsset: { hlsPath: 'content/tenant1/123/trailer/hls/master.m3u8' } // wrong dir for main
            })
        });
        mockSubGet.mockResolvedValueOnce({
            empty: false,
            docs: [{ data: () => ({ expiryDate: { toMillis: () => 2000 } }) }]
        });

        const req = { contentId: '123' };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('INVALID_ASSET_PATH');
    });

    it('rejects main hlsPath when trailer is requested', async () => {
        process.env.CLOUDFRONT_KEY_PAIR_ID = 'key123';
        process.env.CLOUDFRONT_PRIVATE_KEY = 'private123';
        process.env.CLOUDFRONT_DOMAIN = 'd12345.cloudfront.net';

        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({
                status: 'PUBLISHED',
                tenantId: 'tenant1',
                trailerPlaybackAsset: { hlsPath: 'content/tenant1/123/main/hls/master.m3u8' } // wrong dir for trailer
            })
        });

        const req = { contentId: '123', isTrailer: true };
        const context = { auth: { uid: 'user1' } };
        await expect(authorizePlaybackHandler(req as any, context as any)).rejects.toThrow('INVALID_ASSET_PATH');
    });

    // ── Trailer Authorization ─────────────────────────────────────────────────

    it('allows trailer playback without subscription check', async () => {
        process.env.CLOUDFRONT_KEY_PAIR_ID = 'key123';
        process.env.CLOUDFRONT_PRIVATE_KEY = 'private123';
        process.env.CLOUDFRONT_DOMAIN = 'd12345.cloudfront.net';

        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({
                status: 'PUBLISHED',
                tenantId: 'tenant1',
                trailerPlaybackAsset: { hlsPath: 'content/tenant1/123/trailer/hls/trailer.m3u8' }
            })
        });

        const req = { contentId: '123', isTrailer: true };
        const context = { auth: { uid: 'user1' } };
        const result = await authorizePlaybackHandler(req as any, context as any) as any;

        expect(result.authorized).toBe(true);
        expect(result.playbackUrl).toBe('https://d12345.cloudfront.net/content/tenant1/123/trailer/hls/trailer.m3u8');
        // Subscription query must NOT be called for trailers
        expect(mockSubGet).not.toHaveBeenCalled();
        // Cookie scope must be narrowly scoped to trailer directory — not main
        const { getSignedCookies } = require('@aws-sdk/cloudfront-signer');
        expect(getSignedCookies).toHaveBeenCalledWith(expect.objectContaining({
            url: 'https://d12345.cloudfront.net/content/tenant1/123/trailer/hls/*'
        }));
    });

    it('trailer cookie scope must not cover main content path', async () => {
        process.env.CLOUDFRONT_KEY_PAIR_ID = 'key123';
        process.env.CLOUDFRONT_PRIVATE_KEY = 'private123';
        process.env.CLOUDFRONT_DOMAIN = 'd12345.cloudfront.net';

        mockContentGet.mockResolvedValueOnce({
            exists: true,
            data: () => ({
                status: 'PUBLISHED',
                tenantId: 'tenant1',
                trailerPlaybackAsset: { hlsPath: 'content/tenant1/123/trailer/hls/trailer.m3u8' }
            })
        });

        const req = { contentId: '123', isTrailer: true };
        const context = { auth: { uid: 'user1' } };
        await authorizePlaybackHandler(req as any, context as any);

        const { getSignedCookies } = require('@aws-sdk/cloudfront-signer');
        const callArgs = getSignedCookies.mock.calls[0][0];
        // The signed URL must NOT be a wildcard covering the whole content directory
        expect(callArgs.url).not.toContain('/content/*');
        // The signed URL must NOT cover the main asset directory
        expect(callArgs.url).not.toContain('/main/hls/');
    });
});
