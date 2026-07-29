import { cookies } from 'next/headers';

const COOKIE_NAME = 'reality_google_auth';

export async function setTokenCookie(payload: Record<string, unknown>) {
    const cookieStore = await cookies();
    cookieStore.set({
        name: COOKIE_NAME,
        value: JSON.stringify(payload),
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
        return JSON.parse(cookie.value);
    } catch {
        return null;
    }
}

export async function clearTokenCookie() {
    const cookieStore = await cookies();
    cookieStore.delete(COOKIE_NAME);
}
