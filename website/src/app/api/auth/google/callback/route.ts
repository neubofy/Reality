import { NextRequest, NextResponse } from 'next/server';
import { setTokenCookie } from '@/lib/tokenCookie';

export async function GET(req: NextRequest) {
    const searchParams = req.nextUrl.searchParams;
    const code = searchParams.get('code');
    const error = searchParams.get('error');
    const incomingState = searchParams.get('state');

    const origin = req.nextUrl.origin;
    const tapashyaUrl = new URL('/tapashya', origin);

    if (error) {
        console.error('Google OAuth Error:', error);
        return NextResponse.redirect(tapashyaUrl);
    }

    if (!code) {
        return NextResponse.redirect(tapashyaUrl);
    }

    // Verify CSRF state token
    const savedState = req.cookies.get('oauth_state')?.value;
    if (!incomingState || !savedState || incomingState !== savedState) {
        console.error('Google OAuth state mismatch: possible CSRF attempt');
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
        const payload = {
            access_token: tokenData.access_token,
            refresh_token: tokenData.refresh_token,
            expires_at: Date.now() + tokenData.expires_in * 1000,
        };

        await setTokenCookie(payload);

        // Redirect back to Tapashya page and clean up state cookie
        const response = NextResponse.redirect(tapashyaUrl);
        response.cookies.delete('oauth_state');
        return response;
    } catch (err) {
        console.error('OAuth Callback Exception:', err);
        return NextResponse.redirect(tapashyaUrl);
    }
}
