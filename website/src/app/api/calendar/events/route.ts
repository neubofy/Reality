import { NextResponse } from 'next/server';
import { getTokenCookie } from '@/lib/tokenCookie';

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
            return NextResponse.json({ error: 'Token expired or invalid' }, { status: 401 });
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
