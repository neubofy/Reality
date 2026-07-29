import { NextRequest, NextResponse } from 'next/server';
import { setTokenCookie } from '@/lib/tokenCookie';

export async function POST(req: NextRequest) {
    try {
        const body = await req.json();
        const { googleAccessToken } = body;

        if (!googleAccessToken) {
            return NextResponse.json({ error: 'Missing googleAccessToken' }, { status: 400 });
        }

        await setTokenCookie({
            access_token: googleAccessToken,
            expires_at: Date.now() + (3600 * 1000) // Note: Google access tokens expire in 1 hour
        });

        return NextResponse.json({ success: true });
    } catch (e) {
        console.error("Failed to set session", e);
        return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 });
    }
}
