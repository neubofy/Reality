'use client';

import React from 'react';

export default function CopyPromptButton() {
  return (
    <button 
      onClick={() => {
          navigator.clipboard.writeText(`Help me set up a Google Cloud Console project for a local-first Android app called "Reality".\n\nWalk me through these steps one by one. Don't give me the next step until I say "done".\n\nStep 1: Go to Google Cloud Console and create a new project.\nStep 2: Enable the following APIs: Google Calendar API, Google Tasks API, Google Drive API.\nStep 3: Setup the "OAuth Consent Screen" as "External". Fill in the mandatory app details.\nStep 4: In the "Scopes" section of the consent screen, add exactly these 6 scopes:\n- https://www.googleapis.com/auth/calendar.events\n- https://www.googleapis.com/auth/drive.file\n- https://www.googleapis.com/auth/tasks\n- https://www.googleapis.com/auth/userinfo.email\n- https://www.googleapis.com/auth/userinfo.profile\n- openid\nStep 5: Set the publishing status to "Testing" and add my own email to the "Test Users" list.\nStep 6: Go to "Credentials" -> "Create Credentials" -> "OAuth Client ID".\nStep 7: Choose Application Type: "Desktop app". (This is critical because the app uses a local 127.0.0.1 redirect).\nStep 8: Give me the Client ID and Client Secret.\n\nOnce I have the keys, tell me to open the Reality App -> Go to the Elite Page or Profile Page -> Tap the Settings icon in the top right -> Paste the Client ID and Secret and click Save.`);
          alert('Prompt copied to clipboard!');
      }}
      className="absolute top-4 right-4 bg-neural-purple hover:bg-neural-purple/80 text-white px-3 py-1.5 rounded-lg text-xs font-bold transition-colors shadow-lg flex items-center gap-2"
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
      Copy Prompt
    </button>
  );
}
