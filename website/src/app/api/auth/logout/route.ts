import { NextResponse } from 'next/server';
import { clearTokenCookie } from '@/lib/tokenCookie';

export async function POST() {
    await clearTokenCookie();
    return NextResponse.json({ success: true });
}
