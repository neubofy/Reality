import { NextResponse } from 'next/server';
import { getTokenCookie, setTokenCookie } from '@/lib/tokenCookie';

export async function GET() {
    let tokenData = await getTokenCookie();

    if (!tokenData || !tokenData.access_token) {
        return NextResponse.json({ error: 'Not connected' }, { status: 401 });
    }

    const accessToken = tokenData.access_token;

    try {
        const startOfDay = new Date();
        startOfDay.setHours(0, 0, 0, 0);
        const endOfDay = new Date();
        endOfDay.setHours(23, 59, 59, 999);

        // Fetch events with timeout controller
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000); // 10s timeout

        const res = await fetch(`https://www.googleapis.com/calendar/v3/calendars/primary/events?timeMin=${startOfDay.toISOString()}&timeMax=${endOfDay.toISOString()}&singleEvents=true&orderBy=startTime`, {
            headers: { Authorization: `Bearer ${accessToken}` },
            signal: controller.signal
        });

        clearTimeout(timeoutId);

        if (res.status === 401) {
            // Token expired. Try to use refresh_token.
            if (!tokenData.refresh_token) {
                return NextResponse.json({ error: 'Token expired and no refresh token available' }, { status: 401 });
            }

            const clientId = process.env.GOOGLE_CLIENT_ID;
            const clientSecret = process.env.GOOGLE_CLIENT_SECRET;

            if (!clientId || !clientSecret) {
                return NextResponse.json({ error: 'Server configuration error' }, { status: 500 });
            }

            const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({
                    client_id: clientId,
                    client_secret: clientSecret,
                    refresh_token: tokenData.refresh_token,
                    grant_type: 'refresh_token',
                }),
            });

            if (!tokenResponse.ok) {
                return NextResponse.json({ error: 'Failed to refresh token' }, { status: 401 });
            }

            const newTokens = await tokenResponse.json();

            // Update the cookie with the new access token
            tokenData.access_token = newTokens.access_token;
            if (newTokens.refresh_token) {
                tokenData.refresh_token = newTokens.refresh_token;
            }
            tokenData.expires_at = Date.now() + newTokens.expires_in * 1000;
            
            await setTokenCookie(tokenData);

            // Retry the original request with the new token
            const retryRes = await fetch(`https://www.googleapis.com/calendar/v3/calendars/primary/events?timeMin=${startOfDay.toISOString()}&timeMax=${endOfDay.toISOString()}&singleEvents=true&orderBy=startTime`, {
                headers: { Authorization: `Bearer ${tokenData.access_token}` },
            });

            if (!retryRes.ok) {
                 return NextResponse.json({ error: 'Failed to fetch calendar after refresh' }, { status: 500 });
            }
            
            // We successfully retried, so we will assign the retryRes to the original res variable flow.
            // But since res is const, we have to parse the data right here and return.
            const data = await retryRes.json();
            const events = (data.items || [])
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                .filter((e: any) => e.start?.dateTime && e.end?.dateTime)
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                .map((e: any) => ({
                    id: e.id,
                    title: e.summary || 'Study Session',
                    startTime: new Date(e.start.dateTime).getTime(),
                    endTime: new Date(e.end.dateTime).getTime()
                }));

            return NextResponse.json({ events });
        }

        if (!res.ok) {
            return NextResponse.json({ error: 'Failed to fetch calendar' }, { status: 500 });
        }

        const data = await res.json();
        const events = (data.items || [])
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            .filter((e: any) => e.start?.dateTime && e.end?.dateTime)
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            .map((e: any) => ({
                id: e.id,
                title: e.summary || 'Study Session',
                startTime: new Date(e.start.dateTime).getTime(),
                endTime: new Date(e.end.dateTime).getTime()
            }));

        return NextResponse.json({ events });

    } catch (err: unknown) {
        if (err instanceof Error && err.name === 'AbortError') {
            return NextResponse.json({ error: 'Request timeout' }, { status: 504 });
        }
        console.error("Calendar fetch failed"); // Safe log without token info
        return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 });
    }
}
