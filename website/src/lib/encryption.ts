import crypto from 'crypto';

const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 16;
const SALT_LENGTH = 16;
const TAG_LENGTH = 16;

/**
 * Derives a 32-byte key from the provided secret using scrypt.
 * This ensures the key is exactly 32 bytes for aes-256-gcm.
 */
const getKey = (secret: string, salt: Buffer): Buffer => {
    return crypto.scryptSync(secret, salt, 32);
};

/**
 * Encrypts a string using AES-256-GCM.
 * The output format is a single base64 string containing:
 * base64(salt + iv + tag + encrypted_data)
 */
export const encryptString = (text: string): string => {
    const secret = process.env.SESSION_SECRET;
    if (!secret) {
        throw new Error('SESSION_SECRET environment variable is not defined');
    }

    const salt = crypto.randomBytes(SALT_LENGTH);
    const iv = crypto.randomBytes(IV_LENGTH);
    const key = getKey(secret, salt);

    const cipher = crypto.createCipheriv(ALGORITHM, key, iv);
    
    let encrypted = cipher.update(text, 'utf8', 'base64');
    encrypted += cipher.final('base64');
    
    const tag = cipher.getAuthTag();

    // Combine everything into a single buffer
    const payload = Buffer.concat([
        salt,
        iv,
        tag,
        Buffer.from(encrypted, 'base64')
    ]);

    return payload.toString('base64');
};

/**
 * Decrypts a base64 string that was encrypted with encryptString.
 */
export const decryptString = (encryptedPayload: string): string => {
    const secret = process.env.SESSION_SECRET;
    if (!secret) {
        throw new Error('SESSION_SECRET environment variable is not defined');
    }

    const payload = Buffer.from(encryptedPayload, 'base64');

    // Extract pieces
    const salt = payload.subarray(0, SALT_LENGTH);
    const iv = payload.subarray(SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
    const tag = payload.subarray(SALT_LENGTH + IV_LENGTH, SALT_LENGTH + IV_LENGTH + TAG_LENGTH);
    const encryptedText = payload.subarray(SALT_LENGTH + IV_LENGTH + TAG_LENGTH);

    const key = getKey(secret, salt);

    const decipher = crypto.createDecipheriv(ALGORITHM, key, iv);
    decipher.setAuthTag(tag);

    let decrypted = decipher.update(encryptedText, undefined, 'utf8');
    decrypted += decipher.final('utf8');

    return decrypted;
};
