import { NextRequest, NextResponse } from 'next/server';

export async function GET(req: NextRequest) {
    const clientId = process.env.GOOGLE_CLIENT_ID;
    if (!clientId) {
        return NextResponse.json({ error: 'Missing GOOGLE_CLIENT_ID' }, { status: 500 });
    }

    // Determine the base URL dynamically based on the request origin
    // This allows it to work seamlessly on localhost and production (reality.neubofy.in)
    const origin = req.nextUrl.origin;
    const redirectUri = `${origin}/api/auth/google/callback`;

    // Define the scopes needed. 
    // The Tapashya page only strictly needs calendar access.
    const scopes = [
        'https://www.googleapis.com/auth/calendar.events',
        'email',
        'profile',
    ].join(' ');

    const params = new URLSearchParams({
        client_id: clientId,
        redirect_uri: redirectUri,
        response_type: 'code',
        scope: scopes,
        access_type: 'offline',     // Critical: requests a refresh token
        prompt: 'consent',          // Critical: forces Google to issue the refresh token every time
    });

    const googleAuthUrl = `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`;

    // Redirect the user to Google's consent screen
    return NextResponse.redirect(googleAuthUrl);
}
