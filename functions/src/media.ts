import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { S3Client, PutObjectCommand, HeadObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { MediaConvertClient, CreateJobCommand } from "@aws-sdk/client-mediaconvert";
import { v4 as uuidv4 } from "uuid";
// @ts-ignore
import MessageValidator = require('sns-validator');

// Configuration from environment variables
const S3_BUCKET = process.env.S3_UPLOAD_BUCKET || "ott-content-bucket";
const AWS_REGION = process.env.AWS_REGION || "us-east-1";
const MEDIACONVERT_ENDPOINT = process.env.MEDIACONVERT_ENDPOINT || "https://mediaconvert.region.amazonaws.com";
const MEDIACONVERT_ROLE = process.env.MEDIACONVERT_ROLE || "arn:aws:iam::account:role/MediaConvertRole";

// Internal clients
const s3Client = new S3Client({ region: AWS_REGION });
const mediaConvertClient = new MediaConvertClient({
    region: AWS_REGION,
    endpoint: MEDIACONVERT_ENDPOINT
});
const db = admin.firestore();

// Limits
const MAX_VIDEO_SIZE = 10 * 1024 * 1024 * 1024; // 10 GB
const MAX_IMAGE_SIZE = 20 * 1024 * 1024; // 20 MB

const ALLOWED_VIDEO_MIMES = ["video/mp4", "video/quicktime"];
const ALLOWED_IMAGE_MIMES = ["image/jpeg", "image/png", "image/webp"];

async function verifyAdmin(uid: string): Promise<string> {
    const userDoc = await db.collection("users").doc(uid).get();
    if (!userDoc.exists) throw new functions.https.HttpsError("permission-denied", "User not found");
    const data = userDoc.data();
    if (!data?.isAdmin) throw new functions.https.HttpsError("permission-denied", "Not an admin");
    if (!data?.tenantId) throw new functions.https.HttpsError("permission-denied", "User has no tenant");
    return data.tenantId;
}

export const createMediaUpload = functions.https.onCall(async (data, context) => {
    if (!context.auth || !context.auth.uid) {
        throw new functions.https.HttpsError("unauthenticated", "Unauthenticated");
    }

    const { contentId, mediaType, fileName, mimeType, fileSize } = data;
    
    if (!contentId || !mediaType || !fileName || !mimeType || fileSize == null) {
        throw new functions.https.HttpsError("invalid-argument", "Missing required fields");
    }

    const tenantId = await verifyAdmin(context.auth.uid);

    const contentDoc = await db.collection("content").doc(contentId).get();
    if (!contentDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Content not found");
    }
    if (contentDoc.data()?.tenantId !== tenantId) {
        throw new functions.https.HttpsError("permission-denied", "Cross-tenant access denied");
    }

    // Validate mediaType & constraints
    if (mediaType === "MAIN_VIDEO" || mediaType === "TRAILER") {
        if (!ALLOWED_VIDEO_MIMES.includes(mimeType)) {
            throw new functions.https.HttpsError("invalid-argument", "Unsupported video MIME type");
        }
        if (fileSize > MAX_VIDEO_SIZE) {
            throw new functions.https.HttpsError("invalid-argument", "File too large");
        }
    } else if (mediaType === "POSTER" || mediaType === "BACKDROP") {
        if (!ALLOWED_IMAGE_MIMES.includes(mimeType)) {
            throw new functions.https.HttpsError("invalid-argument", "Unsupported image MIME type");
        }
        if (fileSize > MAX_IMAGE_SIZE) {
            throw new functions.https.HttpsError("invalid-argument", "File too large");
        }
    } else {
        throw new functions.https.HttpsError("invalid-argument", "Invalid mediaType");
    }

    // Deterministic key generation
    const uploadId = uuidv4();
    const sourceKey = `uploads/${tenantId}/${contentId}/source/${mediaType.toLowerCase()}/${uploadId}`;

    const command = new PutObjectCommand({
        Bucket: S3_BUCKET,
        Key: sourceKey,
        ContentType: mimeType,
    });

    const signedUrl = await getSignedUrl(s3Client, command, { expiresIn: 3600 }); // 1-hour expiry

    const uploadRecord = {
        id: uploadId,
        contentId,
        tenantId,
        mediaType,
        sourceKey,
        status: "PENDING_UPLOAD",
        createdBy: context.auth.uid,
        mimeType,
        fileSize,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };

    await db.collection("mediaUploads").doc(uploadId).set(uploadRecord);

    return {
        uploadId,
        signedUrl,
        sourceKey
    };
});

export const completeMediaUpload = functions.https.onCall(async (data, context) => {
    if (!context.auth || !context.auth.uid) {
        throw new functions.https.HttpsError("unauthenticated", "Unauthenticated");
    }

    const { uploadId } = data;
    if (!uploadId) throw new functions.https.HttpsError("invalid-argument", "Missing uploadId");

    const tenantId = await verifyAdmin(context.auth.uid);

    const uploadRef = db.collection("mediaUploads").doc(uploadId);
    
    return db.runTransaction(async (transaction) => {
        const uploadDoc = await transaction.get(uploadRef);
        if (!uploadDoc.exists) throw new functions.https.HttpsError("not-found", "Upload not found");
        
        const uploadData = uploadDoc.data()!;
        if (uploadData.tenantId !== tenantId) {
            throw new functions.https.HttpsError("permission-denied", "Cross-tenant access denied");
        }

        if (uploadData.status !== "PENDING_UPLOAD" && uploadData.status !== "UPLOADING") {
            // Idempotent completion handling
            return { status: uploadData.status };
        }

        const { contentId, mediaType, sourceKey } = uploadData;

        // Verify that the object actually exists in S3 and matches expected size/type
        try {
            const headCommand = new HeadObjectCommand({
                Bucket: S3_BUCKET,
                Key: sourceKey,
            });
            const headResult = await s3Client.send(headCommand);
            // Verify the actual object size is within a reasonable range of declared size.
            // Allow 5% tolerance for container overhead differences.
            const declaredSize: number = uploadData.fileSize;
            const actualSize = headResult.ContentLength ?? 0;
            const tolerance = Math.max(declaredSize * 0.05, 1024 * 1024); // 5% or 1MB minimum
            if (actualSize === 0) {
                functions.logger.error(`S3 object is empty for key ${sourceKey}`);
                throw new functions.https.HttpsError("failed-precondition", "Uploaded file is empty");
            }
            if (actualSize > declaredSize + tolerance) {
                functions.logger.error(`S3 object size mismatch: declared ${declaredSize}, actual ${actualSize}`);
                throw new functions.https.HttpsError("failed-precondition", "Uploaded file size mismatch");
            }
        } catch (err: any) {
            functions.logger.error(`S3 object not found or inaccessible for key ${sourceKey}`, err);
            throw new functions.https.HttpsError("failed-precondition", "Uploaded file not found in storage");
        }

        if (mediaType === "MAIN_VIDEO" || mediaType === "TRAILER") {
            // Trigger MediaConvert
            // Determine path segment based on media type for security isolation
            const assetDir = mediaType === "MAIN_VIDEO" ? "main" : "trailer";
            const processedPrefix = `content/${tenantId}/${contentId}/${assetDir}/hls/`;
            
            try {
                const mcCommand = new CreateJobCommand({
                    Role: MEDIACONVERT_ROLE,
                    Settings: {
                        OutputGroups: [
                            {
                                Name: "Apple HLS",
                                OutputGroupSettings: {
                                    Type: "HLS_GROUP_SETTINGS",
                                    HlsGroupSettings: {
                                        Destination: `s3://${S3_BUCKET}/${processedPrefix}`,
                                        SegmentLength: 10,
                                        MinSegmentLength: 0
                                    }
                                },
                                Outputs: [
                                    {
                                        VideoDescription: {
                                            CodecSettings: {
                                                Codec: "H_264",
                                                H264Settings: {
                                                    MaxBitrate: 5000000,
                                                    RateControlMode: "QVBR",
                                                    QvbrSettings: {
                                                        QvbrQualityLevel: 7
                                                    }
                                                }
                                            }
                                        },
                                        AudioDescriptions: [
                                            {
                                                CodecSettings: {
                                                    Codec: "AAC",
                                                    AacSettings: {
                                                        Bitrate: 96000,
                                                        CodingMode: "CODING_MODE_2_0",
                                                        SampleRate: 48000
                                                    }
                                                }
                                            }
                                        ],
                                        NameModifier: "_720p"
                                    }
                                ]
                            }
                        ],
                        Inputs: [
                            {
                                FileInput: `s3://${S3_BUCKET}/${sourceKey}`,
                                AudioSelectors: {
                                    "Audio Selector 1": {
                                        DefaultSelection: "DEFAULT"
                                    }
                                }
                            }
                        ]
                    },
                    UserMetadata: {
                        uploadId,
                        contentId,
                        tenantId,
                        mediaType
                    }
                });

                const mcResponse = await mediaConvertClient.send(mcCommand);

                transaction.update(uploadRef, {
                    status: "PROCESSING",
                    jobId: mcResponse.Job?.Id,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });

                return { status: "PROCESSING", jobId: mcResponse.Job?.Id };
            } catch (err: any) {
                functions.logger.error("MediaConvert Trigger failed", err);
                transaction.update(uploadRef, {
                    status: "FAILED",
                    errorCode: err.message,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });
                return { status: "FAILED" };
            }

        } else if (mediaType === "POSTER" || mediaType === "BACKDROP") {
            // Images are READY immediately
            const contentRef = db.collection("content").doc(contentId);
            
            transaction.update(uploadRef, {
                status: "READY",
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            // Logical path, not a presigned URL
            const logicalPath = `s3://${S3_BUCKET}/${sourceKey}`;
            
            if (mediaType === "POSTER") {
                transaction.update(contentRef, {
                    posterUrl: logicalPath,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });
            } else {
                transaction.update(contentRef, {
                    backdropUrl: logicalPath,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });
            }

            return { status: "READY" };
        }
        
        return { status: "FAILED" };
    });
});

const snsValidator = new MessageValidator();

export const mediaConvertWebhook = functions.https.onRequest((req, res) => {
    if (req.method !== 'POST') {
        res.status(405).send('Method Not Allowed');
        return;
    }

    try {
        const body = typeof req.body === 'string' ? JSON.parse(req.body) : req.body;
        
        snsValidator.validate(body, async (err: any, message: any) => {
            if (err) {
                functions.logger.error("SNS signature validation failed", err);
                res.status(401).send('Invalid signature');
                return;
            }

            // Handle SNS Subscription Confirmation
            if (message.Type === 'SubscriptionConfirmation') {
                functions.logger.info(`SNS Subscription Confirmation URL: ${message.SubscribeURL}`);
                try {
                    await fetch(message.SubscribeURL);
                } catch (e) {
                    functions.logger.error("Failed to auto-confirm", e);
                }
                res.status(200).send('OK');
                return;
            }

            if (message.Type !== 'Notification') {
                res.status(400).send('Ignored');
                return;
            }

            // Process MediaConvert Event
            const payload = typeof message.Message === 'string' ? JSON.parse(message.Message) : message.Message;
            
            if (payload.source !== 'aws.mediaconvert') {
                res.status(400).send('Ignored');
                return;
            }

            const status = payload.detail.status; // e.g. COMPLETE, ERROR
            const jobId = payload.detail.jobId;
            const userMetadata = payload.detail.userMetadata;
            
            if (!userMetadata || !userMetadata.uploadId || !jobId) {
                functions.logger.error("Missing userMetadata uploadId or jobId");
                res.status(400).send('Missing metadata');
                return;
            }

            const { uploadId } = userMetadata;
            const uploadRef = db.collection("mediaUploads").doc(uploadId);
            
            await db.runTransaction(async (transaction) => {
                const uploadDoc = await transaction.get(uploadRef);
                if (!uploadDoc.exists) return;
                
                const data = uploadDoc.data()!;
                // Idempotency: Do not process if already finished
                if (data.status === "READY" || data.status === "FAILED") return;
                
                // Stale-job protection
                if (data.jobId && data.jobId !== jobId) {
                    functions.logger.warn(`Stale job event received. Expected: ${data.jobId}, Got: ${jobId}`);
                    return;
                }

                // Correlate trusted data from our own database records
                const contentId = data.contentId;
                const tenantId = data.tenantId;
                const mediaType = data.mediaType;

                if (status === 'COMPLETE') {
                    transaction.update(uploadRef, {
                        status: "READY",
                        updatedAt: admin.firestore.FieldValue.serverTimestamp()
                    });

                    const contentRef = db.collection("content").doc(contentId);
                    // HLS output prefix is derived from trusted fields
                    const assetDir = mediaType === "MAIN_VIDEO" ? "main" : "trailer";
                    const hlsPath = `content/${tenantId}/${contentId}/${assetDir}/hls/${uploadId}.m3u8`;
                    
                    const asset = {
                        contentId,
                        tenantId,
                        assetType: mediaType,
                        hlsPath: hlsPath,
                        duration: 0
                    };

                    if (mediaType === "MAIN_VIDEO") {
                        transaction.update(contentRef, {
                            mainPlaybackAsset: asset,
                            updatedAt: admin.firestore.FieldValue.serverTimestamp()
                        });
                    } else if (mediaType === "TRAILER") {
                        transaction.update(contentRef, {
                            trailerPlaybackAsset: asset,
                            updatedAt: admin.firestore.FieldValue.serverTimestamp()
                        });
                    }
                } else if (status === 'ERROR') {
                    transaction.update(uploadRef, {
                        status: "FAILED",
                        errorCode: payload.detail.errorMessage || 'Transcode failed',
                        updatedAt: admin.firestore.FieldValue.serverTimestamp()
                    });
                }
            });

            res.status(200).send('OK');
        });
    } catch (err) {
        functions.logger.error("Error processing MediaConvert webhook", err);
        res.status(500).send('Internal Server Error');
    }
});

export const changeContentStatusHandler = async (data: any, context: any) => {
    if (!context.auth || !context.auth.uid) {
        throw new functions.https.HttpsError("unauthenticated", "Unauthenticated");
    }

    const { contentId, status } = data;
    if (!contentId || !status) throw new functions.https.HttpsError("invalid-argument", "Missing required arguments");

    // Validate status against an explicit allowlist to prevent arbitrary status injection.
    const ALLOWED_STATUSES = ["PUBLISHED", "DRAFT", "ARCHIVED"];
    if (!ALLOWED_STATUSES.includes(status)) {
        throw new functions.https.HttpsError("invalid-argument", `Invalid status value. Allowed: ${ALLOWED_STATUSES.join(", ")}`);
    }

    const tenantId = await verifyAdmin(context.auth.uid);
    const contentRef = db.collection("content").doc(contentId);
    
    return db.runTransaction(async (transaction) => {
        const contentDoc = await transaction.get(contentRef);
        if (!contentDoc.exists) throw new functions.https.HttpsError("not-found", "Content not found");
        
        const contentData = contentDoc.data()!;
        if (contentData.tenantId !== tenantId) {
            throw new functions.https.HttpsError("permission-denied", "Cross-tenant access denied");
        }

        if (status === "PUBLISHED") {
            // Validate required media readiness
            if (!contentData.mainPlaybackAsset || !contentData.mainPlaybackAsset.hlsPath) {
                throw new functions.https.HttpsError("failed-precondition", "Main video is not READY");
            }

            // Title and description must exist
            if (!contentData.title || !contentData.description) {
                throw new functions.https.HttpsError("failed-precondition", "Metadata incomplete");
            }
        }

        // Proceed to update status
        transaction.update(contentRef, {
            status: status,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        return { success: true };
    });
};

export const changeContentStatus = functions.https.onCall(changeContentStatusHandler);
