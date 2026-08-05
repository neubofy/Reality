import { NextRequest, NextResponse } from 'next/server';
import { setTokenCookie } from '@/lib/tokenCookie';

export async function GET(req: NextRequest) {
    const searchParams = req.nextUrl.searchParams;
    const code = searchParams.get('code');
    const error = searchParams.get('error');

    const origin = req.nextUrl.origin;
    const tapashyaUrl = new URL('/tapashya', origin);

    if (error) {
        console.error('Google OAuth Error:', error);
        return NextResponse.redirect(tapashyaUrl);
    }

    if (!code) {
        return NextResponse.redirect(tapashyaUrl);
    }

    const clientId = process.env.GOOGLE_CLIENT_ID;
    const clientSecret = process.env.GOOGLE_CLIENT_SECRET;

    if (!clientId || !clientSecret) {
        console.error('Missing GOOGLE_CLIENT_ID or GOOGLE_CLIENT_SECRET');
        return NextResponse.json({ error: 'Server configuration error' }, { status: 500 });
    }

    const redirectUri = `${origin}/api/auth/google/callback`;

    try {
        // Exchange the authorization code for tokens
        const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                client_id: clientId,
                client_secret: clientSecret,
                code: code,
                grant_type: 'authorization_code',
                redirect_uri: redirectUri,
            }),
        });

        const tokenData = await tokenResponse.json();

        if (!tokenResponse.ok) {
            console.error('Failed to exchange token:', tokenData);
            return NextResponse.redirect(tapashyaUrl);
        }

        // Store the tokens securely in the encrypted HTTP-only cookie
        // We only overwrite the refresh token if Google provided a new one.
        const payload = {
            access_token: tokenData.access_token,
            refresh_token: tokenData.refresh_token, // This may be undefined on subsequent logins without prompt=consent, but we forced prompt=consent
            expires_at: Date.now() + tokenData.expires_in * 1000,
        };

        await setTokenCookie(payload);

        return NextResponse.redirect(tapashyaUrl);
    } catch (err) {
        console.error('OAuth Callback Exception:', err);
        return NextResponse.redirect(tapashyaUrl);
    }
}
