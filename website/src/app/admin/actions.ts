'use server';

import { cookies } from 'next/headers';
import crypto from 'crypto';

const COOKIE_NAME = 'reality_admin_session';

function hashSecret(secret: string): string {
  return crypto.createHash('sha256').update(secret).digest('hex');
}

export async function loginAction(formData: FormData) {
  const username = formData.get('username') as string;
  const password = formData.get('password') as string;

  const adminSecret = process.env.ADMIN || '';
  if (!adminSecret || adminSecret.length < 17) {
    return { success: false, error: 'Admin configuration missing on server' };
  }

  const expectedAdminUsername = adminSecret.substring(0, 16);
  const expectedAdminPassword = adminSecret.substring(16);

  if (
    username &&
    password &&
    username === expectedAdminUsername &&
    password === expectedAdminPassword
  ) {
    const sessionToken = hashSecret(adminSecret);
    const cookieStore = await cookies();
    cookieStore.set(COOKIE_NAME, sessionToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      path: '/admin',
      maxAge: 60 * 60 * 12, // 12 hours
    });

    return { success: true };
  } else {
    return { success: false, error: 'Invalid credentials' };
  }
}

export async function verifyAdminSession(): Promise<boolean> {
  const adminSecret = process.env.ADMIN || '';
  if (!adminSecret || adminSecret.length < 17) return false;

  const cookieStore = await cookies();
  const token = cookieStore.get(COOKIE_NAME)?.value;
  if (!token) return false;

  const expectedHash = hashSecret(adminSecret);
  try {
    return crypto.timingSafeEqual(Buffer.from(token), Buffer.from(expectedHash));
  } catch {
    return false;
  }
}

export async function logoutAction() {
  const cookieStore = await cookies();
  cookieStore.delete(COOKIE_NAME);
  return { success: true };
}
