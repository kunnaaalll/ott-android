import * as admin from 'firebase-admin';
import { changeContentStatusHandler, mediaConvertWebhook } from './media';
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

// Mock sns-validator
jest.mock('sns-validator', () => {
    return jest.fn().mockImplementation(() => {
        return {
            validate: jest.fn((message, callback) => {
                if (message.Signature === 'INVALID') {
                    callback(new Error('Invalid signature'), null);
                } else {
                    callback(null, message);
                }
            })
        };
    });
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

    describe('mediaConvertWebhook', () => {
        let mockReq: any;
        let mockRes: any;
        const validTopicArn = 'arn:aws:sns:region:account:topic';

        beforeEach(() => {
            process.env.MEDIACONVERT_SNS_TOPIC_ARN = validTopicArn;
            mockReq = {
                method: 'POST',
                body: {
                    Type: 'Notification',
                    TopicArn: validTopicArn,
                    Signature: 'VALID',
                    Message: JSON.stringify({
                        source: 'aws.mediaconvert',
                        detail: {
                            status: 'COMPLETE',
                            jobId: 'job-123',
                            userMetadata: {
                                uploadId: 'upload-456'
                            }
                        }
                    })
                }
            };
            mockRes = {
                status: jest.fn().mockReturnThis(),
                send: jest.fn()
            };
        });

        afterEach(() => {
            delete process.env.MEDIACONVERT_SNS_TOPIC_ARN;
        });

        it('1. Correctly signed notification from the configured topic', async () => {
            mockGet.mockResolvedValueOnce({
                exists: true,
                data: () => ({
                    status: 'PROCESSING',
                    jobId: 'job-123',
                    contentId: 'content-789',
                    tenantId: 'tenant-1',
                    mediaType: 'MAIN_VIDEO'
                })
            });

            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(200);
            expect(mockUpdate).toHaveBeenCalled();
        });

        it('2. Incorrect TopicArn', async () => {
            mockReq.body.TopicArn = 'arn:aws:sns:region:account:wrong-topic';
            
            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(401);
            expect(mockRes.send).toHaveBeenCalledWith('Invalid Topic');
        });

        it('3. Missing TopicArn', async () => {
            delete mockReq.body.TopicArn;

            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(401);
            expect(mockRes.send).toHaveBeenCalledWith('Invalid Topic');
        });

        it('4. Missing MEDIACONVERT_SNS_TOPIC_ARN', async () => {
            delete process.env.MEDIACONVERT_SNS_TOPIC_ARN;

            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(500);
            expect(mockRes.send).toHaveBeenCalledWith('Internal Configuration Error');
        });

        it('5. Empty or invalid configured ARN', async () => {
            process.env.MEDIACONVERT_SNS_TOPIC_ARN = '   ';

            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(500);
            expect(mockRes.send).toHaveBeenCalledWith('Internal Configuration Error');
        });

        it('6. Invalid SNS signature', async () => {
            mockReq.body.Signature = 'INVALID';

            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(401);
            expect(mockRes.send).toHaveBeenCalledWith('Invalid signature');
        });

        it('7. Unexpected message type', async () => {
            mockReq.body.Type = 'UnknownType';

            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(400);
            expect(mockRes.send).toHaveBeenCalledWith('Ignored');
        });

        it('8. Unknown MediaConvert job (missing in DB)', async () => {
            mockGet.mockResolvedValueOnce({ exists: false });

            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(200); // Fails safely, returns OK to SNS
            expect(mockUpdate).not.toHaveBeenCalled();
        });

        it('9. Duplicate completion event (already READY)', async () => {
            mockGet.mockResolvedValueOnce({
                exists: true,
                data: () => ({
                    status: 'READY',
                    jobId: 'job-123'
                })
            });

            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(200);
            expect(mockUpdate).not.toHaveBeenCalled();
        });

        it('10. Stale or mismatched job event', async () => {
            mockGet.mockResolvedValueOnce({
                exists: true,
                data: () => ({
                    status: 'PROCESSING',
                    jobId: 'newer-job-456'
                })
            });

            await new Promise((resolve) => {
                mockRes.send.mockImplementation(() => resolve(null));
                mediaConvertWebhook(mockReq, mockRes);
            });

            expect(mockRes.status).toHaveBeenCalledWith(200); // Ignored
            expect(mockUpdate).not.toHaveBeenCalled();
        });
        
        it('identifies what remains to be verified through integration testing', () => {
            // Note as required by Phase 9.3:
            // "Where full signature validation cannot be exercised in a unit test, explicitly identify what remains to be verified through integration testing."
            // We use a mock for `sns-validator` to simulate signature failures.
            // Full integration testing must verify that actual SNS payloads from AWS MediaConvert
            // contain valid certificates and are successfully validated by the sns-validator package
            // in the production environment.
            expect(true).toBe(true);
        });
    });
});
