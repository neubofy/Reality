"use server";

/**
 * Fetches membership status and expiry details for a specific 16-character User ID.
 * Purely user-facing and read-only; no administrative privileges or backdoors.
 */
export async function fetchSensitiveMemberData(userId?: string) {
  if (!userId || userId.trim().length !== 16) {
    return { error: "A valid 16-character User ID is required." };
  }

  const baseUrl = process.env.Pro_Members_DB_URL;
  if (!baseUrl) {
    return { error: "Pro_Members_DB_URL not defined" };
  }
  const dbUrl = `${baseUrl.replace(/\/+$/, '')}/api/pro-members?userId=${encodeURIComponent(userId.trim())}`;
  const workerSecret = process.env.WORKER_CONNECTION_SECRET || '';

  try {
    let res = await fetch(dbUrl, {
      method: 'GET',
      headers: {
        'x-worker-secret': workerSecret
      },
      next: { revalidate: 60 } // Cache this fetch for 60 seconds
    });

    if (res.status === 302 || res.status === 303 || res.status === 307 || res.status === 308) {
      const redirectUrl = res.headers.get('location');
      if (redirectUrl) {
        res = await fetch(redirectUrl, {
          method: 'GET',
          headers: {
            'x-worker-secret': workerSecret
          },
          next: { revalidate: 60 }
        });
      }
    }

    if (!res.ok) {
      return { error: `Failed to fetch pro member details: ${res.status}` };
    }

    const data = await res.json();
    const members = data?.members || [];

    // Filter to build a dictionary of membership data
    const result: Record<string, { status: string | null, expiryDate: string | null, trial_plan: string | null }> = {};

    for (const m of members) {
      result[m.userId] = {
        status: m.status || null,
        expiryDate: m.expiryDate || null,
        trial_plan: m.trial_plan || null
      };
    }

    return { data: result, members: members, totalMembers: data?.totalMembers || 0 };
  } catch (error: any) {
    return { error: error.message };
  }
}

/**
 * Verifies if a user ID is registered as an Elite/Pro member and retrieves public certificate details.
 */
export async function verifyMemberId(userId: string) {
  if (!userId || userId.trim().length !== 16) {
    return { error: "User ID must be exactly 16 characters." };
  }

  const baseUrl = process.env.Pro_Members_DB_URL;
  if (!baseUrl) {
    return { error: "Pro_Members_DB_URL not defined" };
  }
  const dbUrl = `${baseUrl.replace(/\/+$/, '')}/api/pro-members?userId=${encodeURIComponent(userId.trim())}`;
  const workerSecret = process.env.WORKER_CONNECTION_SECRET || '';

  try {
    let res = await fetch(dbUrl, {
      method: 'GET',
      headers: {
        'x-worker-secret': workerSecret
      },
      next: { revalidate: 60 }
    });

    if (res.status === 302 || res.status === 303 || res.status === 307 || res.status === 308) {
      const redirectUrl = res.headers.get('location');
      if (redirectUrl) {
        res = await fetch(redirectUrl, {
          method: 'GET',
          headers: {
            'x-worker-secret': workerSecret
          },
          next: { revalidate: 60 }
        });
      }
    }

    if (!res.ok) {
      return { error: `Failed to query member database: ${res.status}` };
    }

    const data = await res.json();
    const members = data?.members || [];
    const found = members.find((m: any) => m.userId.toLowerCase() === userId.trim().toLowerCase());

    if (found) {
      return {
        success: true,
        member: {
          userId: found.userId,
          dateJoined: found.date || found.dateJoined || '',
          hasAccess: found.status === 'V' || (!found.status && !!found.trial_plan),
          status: found.status || null,
          expiryDate: found.expiryDate || null,
          trial_plan: found.trial_plan || null,
        }
      };
    }
    return { success: false, error: "User ID not found in our records." };
  } catch (err: any) {
    return { error: err.message };
  }
}
