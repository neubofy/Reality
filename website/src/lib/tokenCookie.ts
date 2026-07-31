import { cookies } from 'next/headers';
import { encryptString, decryptString } from './encryption';

const COOKIE_NAME = 'reality_google_auth';

export async function setTokenCookie(payload: Record<string, unknown>) {
    const cookieStore = await cookies();
    const jsonPayload = JSON.stringify(payload);
    const encryptedValue = encryptString(jsonPayload);

    cookieStore.set({
        name: COOKIE_NAME,
        value: encryptedValue,
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        path: '/',
        maxAge: 30 * 24 * 60 * 60, // 30 days
    });
}

export async function getTokenCookie() {
    const cookieStore = await cookies();
    const cookie = cookieStore.get(COOKIE_NAME);
    if (!cookie?.value) return null;

    try {
        const decryptedJson = decryptString(cookie.value);
        return JSON.parse(decryptedJson);
    } catch (e) {
        console.error('Failed to decrypt or parse token cookie', e);
        return null;
    }
}

export async function clearTokenCookie() {
    const cookieStore = await cookies();
    cookieStore.delete(COOKIE_NAME);
}
