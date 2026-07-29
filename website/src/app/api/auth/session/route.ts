import { NextRequest, NextResponse } from 'next/server';
import { setTokenCookie } from '@/lib/tokenCookie';

export async function POST(req: NextRequest) {
    try {
        const body = await req.json();
        const { googleAccessToken } = body;

        if (!googleAccessToken) {
            return NextResponse.json({ error: 'Missing googleAccessToken' }, { status: 400 });
        }

        // Store the Google Access Token securely in an HTTP-only cookie
        await setTokenCookie({
            access_token: googleAccessToken,
            // Note: Firebase Web SDK doesn't provide a refresh token by default for Google providers
            // If the access token expires, the user will need to re-authenticate via Firebase popup
            expires_at: Date.now() + (3600 * 1000) // Rough 1 hr expiration estimate for Google tokens
        });

        return NextResponse.json({ success: true });
    } catch (e) {
        console.error("Failed to set session", e);
        return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 });
    }
}
