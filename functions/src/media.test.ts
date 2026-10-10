import * as admin from 'firebase-admin';
import { changeContentStatusHandler } from './media';
import 'jest';

// Mock firebase-admin
jest.mock('firebase-admin', () => {
    const mockGet = jest.fn();
    const mockUpdate = jest.fn();
    const mockSet = jest.fn();
    
    return {
        firestore: Object.assign(jest.fn().mockReturnValue({
            collection: jest.fn().mockImplementation((col) => {
                if (col === 'content') {
                    return { doc: jest.fn().mockReturnValue({ get: mockGet, update: mockUpdate, set: mockSet }) };
                }
                if (col === 'users') {
                    return { doc: jest.fn().mockReturnValue({ get: mockGet }) };
                }
                return { doc: jest.fn().mockReturnValue({ get: mockGet, update: mockUpdate, set: mockSet }) };
            }),
            runTransaction: jest.fn().mockImplementation(async (callback) => {
                const transaction = {
                    get: mockGet,
                    update: mockUpdate,
                    set: mockSet
                };
                return callback(transaction);
            })
        }), {
            FieldValue: {
                serverTimestamp: jest.fn().mockReturnValue('mock-timestamp')
            }
        })
    };
});

describe('media.ts Tests', () => {
    let mockGet: jest.Mock;
    let mockUpdate: jest.Mock;

    beforeEach(() => {
        jest.clearAllMocks();
        const db = admin.firestore();
        mockGet = db.collection('content').doc().get as jest.Mock;
        mockUpdate = db.collection('content').doc().update as jest.Mock;
    });

    describe('changeContentStatusHandler', () => {
        it('rejects unauthenticated users', async () => {
            const context = { auth: undefined };
            const req = { contentId: '123', status: 'PUBLISHED' };
            await expect(changeContentStatusHandler(req as any, context as any)).rejects.toThrow('Unauthenticated');
        });

        it('rejects cross-tenant publish', async () => {
            const context = { auth: { uid: 'user1' } };
            const req = { contentId: '123', status: 'PUBLISHED' };
            
            mockGet.mockResolvedValueOnce({ exists: true, data: () => ({ isAdmin: true, tenantId: 'tenant1' }) }); // User
            mockGet.mockResolvedValueOnce({ exists: true, data: () => ({ tenantId: 'tenant2' }) }); // Content
            
            await expect(changeContentStatusHandler(req as any, context as any)).rejects.toThrow('Cross-tenant access denied');
        });

        it('rejects publish if main media is not READY (no mainPlaybackAsset)', async () => {
            const context = { auth: { uid: 'user1' } };
            const req = { contentId: '123', status: 'PUBLISHED' };
            
            mockGet.mockResolvedValueOnce({ exists: true, data: () => ({ isAdmin: true, tenantId: 'tenant1' }) }); // User
            mockGet.mockResolvedValueOnce({ exists: true, data: () => ({ tenantId: 'tenant1', title: 'T', description: 'D' }) }); // Content without mainPlaybackAsset
            
            await expect(changeContentStatusHandler(req as any, context as any)).rejects.toThrow('Main video is not READY');
        });

        it('allows publish when main media is READY and user is admin', async () => {
            const context = { auth: { uid: 'user1' } };
            const req = { contentId: '123', status: 'PUBLISHED' };
            
            mockGet.mockResolvedValueOnce({ exists: true, data: () => ({ isAdmin: true, tenantId: 'tenant1' }) }); // User
            mockGet.mockResolvedValueOnce({ 
                exists: true, 
                data: () => ({ 
                    tenantId: 'tenant1', 
                    title: 'T', 
                    description: 'D',
                    mainPlaybackAsset: { hlsPath: 'path.m3u8' }
                }) 
            }); // Content
            
            const result = await changeContentStatusHandler(req as any, context as any);
            expect(result).toEqual({ success: true });
            expect(mockUpdate).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ status: 'PUBLISHED' }));
        });
    });
});
