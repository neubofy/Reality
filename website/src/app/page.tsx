import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import HeroActions from './HeroActions';
import ScreenshotGallery from './ScreenshotGallery';
import EcosystemAnimation from './EcosystemAnimation';
import ArchitectureBlueprints from './ArchitectureBlueprints';
import CopyPromptButton from './CopyPromptButton';

import { 
  Download, 
  Star, 
  Shield, 
  Lock, 
  Brain, 
  Smartphone, 
  Database, 
  HeartPulse, 
  Moon, 
  Zap, 
  CheckCircle, 
  Crosshair, 
  Target, 
  EyeOff, 
  Layout, 
  FileText, 
  SmartphoneCharging, 
  Cpu,
  ArrowUpRight,
  Code,
  TrendingUp,
  Activity,
  Layers,
  RefreshCw,
  FolderLock,
  Github,
  AlertCircle
} from 'lucide-react';

export default async function Home() {

  let latestVersion = "1.0.9";
  let downloadCount = "1000+";

  try {
      const res = await fetch('https://api.github.com/repos/neubofy/Reality/releases', {
          next: { revalidate: 360 }
      });
      if (res.ok) {
          const releases = await res.json();
          if (releases && releases.length > 0) {
              latestVersion = releases[0].name || latestVersion;
              let totalDownloads = 0;
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              releases.forEach((release: any) => {
                  if (release.assets) {
                      // eslint-disable-next-line @typescript-eslint/no-explicit-any
                      release.assets.forEach((asset: any) => {
                          totalDownloads += asset.download_count;
                      });
                  }
              });
              downloadCount = totalDownloads.toString() + "+";
          }
      }
  } catch (e) {
      console.error("Failed to fetch release info", e);
  }

  return (
    <div className="min-h-screen bg-neural-bg font-outfit text-gray-100 selection:bg-neural-cyan selection:text-black overflow-x-hidden">
      
      {/* Structured Data for Google Verification Crawlers */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            "@context": "https://schema.org",
            "@type": "SoftwareApplication",
            "name": "Reality",
            "operatingSystem": "Android",
            "applicationCategory": "Productivity",
            "publisher": {
              "@type": "Organization",
              "name": "Neubofy",
              "url": "https://reality.neubofy.in"
            },
            "description": "Reality is a highly private, local-first productivity operating system. 100% source-available with transparent AI powered by GPT-OSS models. All code auditable on GitHub. Zero developer backend servers.",
            "url": "https://reality.neubofy.in",
            "author": {
              "@type": "Organization",
              "name": "Neubofy"
            }
          })
        }}
      />
      
      {/* Premium Hero Section */}
      <header id="hero-section" className="relative overflow-hidden border-b border-gray-800 py-20 lg:py-32">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-neural-purple/20 via-neural-bg to-neural-bg opacity-50 z-0"></div>
        
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="grid lg:grid-cols-12 gap-12 items-center">
            
            {/* Left Headline Column */}
            <div className="lg:col-span-7 text-left space-y-6">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-neural-cyan/30 bg-neural-cyan/10 text-neural-cyan text-xs font-mono tracking-widest uppercase">
                 <span>100% SOURCE AVAILABLE</span>
                 <span className="w-1.5 h-1.5 rounded-full bg-neural-cyan animate-pulse"></span>
              </div>
              
              <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight text-white leading-none">
                Take Command of Your Focus with <span className="text-transparent bg-clip-text bg-gradient-to-r from-neural-cyan to-neural-purple">Reality</span>
              </h1>
              
              <h2 className="text-xl md:text-2xl font-medium text-gray-400 font-mono">
                The Best Focus & Discipline App | Military-Grade Android Blocker | 100% Transparent
              </h2>
              
              <p className="text-gray-400 text-lg max-w-xl leading-relaxed">
                Reality is not another bypassable app timer. It's a zero-server, local-first productivity operating system with transparent AI (GPT-OSS 20B & 120B), secure on-device analytics, BYOC sync, and 100% auditable source code.
              </p>

              <div className="flex flex-wrap gap-4 pt-2">
                 <div className="flex items-center gap-2 text-gray-400 bg-neural-card/50 px-4 py-2 rounded-lg border border-gray-800">
                    <Download size={18} className="text-neural-cyan" />
                    <span className="font-mono text-sm">{downloadCount} Downloads</span>
                 </div>
                 <div className="flex items-center gap-2 text-gray-400 bg-neural-card/50 px-4 py-2 rounded-lg border border-gray-800">
                    <Star size={18} className="text-neural-purple" />
                    <span className="font-mono text-sm">v{latestVersion}</span>
                 </div>
                 <div className="flex items-center gap-2 text-gray-400 bg-neural-card/50 px-4 py-2 rounded-lg border border-gray-800">
                    <Github size={18} className="text-neural-cyan" />
                    <span className="font-mono text-sm">Open Source</span>
                 </div>
              </div>

              <div className="flex flex-col sm:flex-row gap-4 pt-4">
                <HeroActions latestVersion={latestVersion} />
              </div>
            </div>

            {/* Right Mockup Display Column */}
            <div className="lg:col-span-5 flex justify-center">
              <div className="relative group max-w-sm w-full">
                <div className="absolute -inset-1 bg-gradient-to-r from-neural-cyan to-neural-purple rounded-[32px] blur-lg opacity-30 group-hover:opacity-75 transition duration-1000"></div>
                <div className="relative rounded-[28px] border border-gray-700 bg-neural-card p-3 shadow-2xl">
                  <Image
                    src="/dashboard_mockup.png"
                    alt="Reality Life OS Dashboard Mockup showcasing focus statistics and AMOLED-optimized productivity scores"
                    width={400}
                    height={800}
                    priority
                    className="rounded-[20px] w-full h-auto border border-gray-800 shadow-inner bg-black"
                  />
                  <div className="absolute bottom-6 left-1/2 transform -translate-x-1/2 bg-black/85 backdrop-blur border border-gray-800 px-4 py-2 rounded-full flex items-center gap-2 shadow-lg">
                    <Shield size={14} className="text-neural-cyan" />
                    <span className="text-xs font-mono text-gray-300">Local-First Guard Active</span>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
      </header>

      {/* 100% Source Available Banner */}
      <section className="py-12 bg-gradient-to-r from-neural-cyan/10 to-neural-purple/10 border-b border-gray-800">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-start gap-4">
            <AlertCircle className="text-neural-cyan shrink-0 mt-1" size={24} />
            <div>
              <h3 className="text-xl font-bold text-white mb-2">🚀 Fully Transparent Architecture</h3>
              <p className="text-gray-300 text-sm leading-relaxed">
                Every line of code is open for audit. Our AI workers run on Cloudflare, the website is Next.js, APK builds are automated via GitHub Actions, and subscription management is auditable. <strong>Zero hidden servers. Zero data collection.</strong> Your focus metrics, journals, and calendar data stay on your device or in your personal Google Cloud—never touch Neubofy servers.
              </p>
              <div className="mt-3 flex flex-wrap gap-2">
                <a href="https://github.com/neubofy/Reality" target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-2 text-neural-cyan hover:text-neural-cyan/80 font-mono text-xs bg-neural-card/50 px-3 py-1 rounded border border-neural-cyan/20">
                  <Github size={14} /> View Source on GitHub
                </a>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Feature Screenshot Showcases (Simulator Gallery) */}
      <section id="screenshots-gallery" className="py-24 bg-neural-card/10 border-b border-gray-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16 space-y-4">
            <span className="text-xs uppercase font-mono tracking-widest text-neural-cyan font-bold">App Interface Gallery</span>
            <h2 className="text-3xl md:text-5xl font-extrabold text-white">AMOLED Cinematic User Interface</h2>
            <p className="text-gray-400 max-w-2xl mx-auto text-sm">Visual mockups of the principal modules running inside Reality.</p>
          </div>

          <ScreenshotGallery />
        </div>
      </section>

      {/* The Ecosystem Animation Section */}
      <EcosystemAnimation />

      <ArchitectureBlueprints />

      {/* Zero-Trust Security */}
      <section id="zero-trust" className="py-24 bg-neural-bg border-b border-gray-800">
         <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-16 space-y-4">
                <span className="text-xs uppercase font-mono tracking-widest text-neural-cyan font-bold">Data Privacy Model</span>
                <h2 className="text-3xl font-bold text-white">Direct Local-First Architecture</h2>
                <p className="text-gray-400 text-sm font-mono">No servers between your device and your personal cloud storage.</p>
            </div>

            <div className="grid md:grid-cols-2 gap-12">
               <div className="space-y-6">
                  <h3 className="text-xl font-semibold text-neural-cyan flex items-center gap-2">
                     <Shield size={20} /> On-Device Privacy
                  </h3>
                  <p className="text-gray-400 text-sm leading-relaxed">
                     All credentials, usage databases, and authentication logs are saved locally inside EncryptedSharedPreferences and an encrypted SQLite database. You configure your own personal Google Cloud credentials.
                  </p>
                  <div className="pt-2">
                    <a href="https://github.com/neubofy/Reality/blob/main/app/src/main/java/com/neubofy/reality/google/GoogleAuthManager.kt" target="_blank" rel="noopener noreferrer" className="text-neural-cyan hover:text-white text-sm font-mono flex items-center gap-1 transition-colors">
                      GoogleAuthManager.kt <ArrowUpRight size={12}/>
                    </a>
                  </div>
               </div>

               <div className="space-y-6">
                  <h3 className="text-xl font-semibold text-neural-purple flex items-center gap-2">
                     <Lock size={20} /> Bypass & Tamper Prevention
                  </h3>
                  <p className="text-gray-400 text-sm leading-relaxed">
                     Reality prevents time manipulation and force-stops using background verification loops. If a settings-level override attempt is detected, Strict Mode blocks the request and logs the incident.
                  </p>
                  <div className="pt-2">
                    <a href="https://github.com/neubofy/Reality/blob/main/app/src/main/java/com/neubofy/reality/ui/activity/StrictModeActivity.kt" target="_blank" rel="noopener noreferrer" className="text-neural-cyan hover:text-white text-sm font-mono flex items-center gap-1 transition-colors">
                      StrictModeActivity.kt <ArrowUpRight size={12}/>
                    </a>
                  </div>
               </div>
            </div>
         </div>
      </section>

      {/* AI Architecture Section */}
      <section id="ai-architecture" className="py-24 bg-neural-card/30 border-b border-gray-800">
         <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-12">
               <h2 className="text-3xl font-extrabold text-white">Reality Intelligence Assistant</h2>
               <p className="text-gray-400 mt-2">Open-Source AI with Transparent Edge Acceleration</p>
            </div>

            <div className="bg-neural-card p-4 sm:p-8 rounded-2xl border border-gray-800 shadow-lg space-y-6">
               <div className="grid md:grid-cols-2 gap-8">
                  <div className="space-y-4">
                     <h3 className="text-xl font-bold text-neural-cyan flex items-center gap-2">
                        <Brain size={24} /> On-Device Models
                     </h3>
                     <p className="text-gray-300 text-sm leading-relaxed">
                        Reality runs open-source GPT-OSS 20B and 120B models directly on your device. Your conversations, journal reflections, and task requests never leave your phone.
                     </p>
                     <ul className="space-y-2 text-gray-400 text-sm">
                        <li className="flex items-start gap-2">
                           <CheckCircle size={16} className="text-neural-cyan mt-0.5 shrink-0" />
                           <span>Complete data privacy</span>
                        </li>
                        <li className="flex items-start gap-2">
                           <CheckCircle size={16} className="text-neural-cyan mt-0.5 shrink-0" />
                           <span>Works 100% offline</span>
                        </li>
                        <li className="flex items-start gap-2">
                           <CheckCircle size={16} className="text-neural-cyan mt-0.5 shrink-0" />
                           <span>No telemetry or logging</span>
                        </li>
                     </ul>
                  </div>

                  <div className="space-y-4">
                     <h3 className="text-xl font-bold text-neural-purple flex items-center gap-2">
                        <Zap size={24} /> Cloudflare Edge Acceleration
                     </h3>
                     <p className="text-gray-300 text-sm leading-relaxed">
                        For faster inference on complex tasks, Reality optionally leverages Cloudflare Workers for JIT cryptography and model acceleration. <strong>No data is stored or logged on edge servers.</strong>
                     </p>
                     <ul className="space-y-2 text-gray-400 text-sm">
                        <li className="flex items-start gap-2">
                           <CheckCircle size={16} className="text-neural-purple mt-0.5 shrink-0" />
                           <span>Speed optimization only</span>
                        </li>
                        <li className="flex items-start gap-2">
                           <CheckCircle size={16} className="text-neural-purple mt-0.5 shrink-0" />
                           <span>Open-source Workers code</span>
                        </li>
                        <li className="flex items-start gap-2">
                           <CheckCircle size={16} className="text-neural-purple mt-0.5 shrink-0" />
                           <span>Fully auditable on GitHub</span>
                        </li>
                     </ul>
                  </div>
               </div>

               <div className="mt-8 pt-8 border-t border-gray-800">
                  <p className="text-gray-400 text-sm mb-4">
                     All infrastructure code, AI worker implementations, and encryption logic are visible in the GitHub repository for independent security audits.
                  </p>
                  <a href="https://github.com/neubofy/Reality" target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-2 text-neural-cyan hover:text-white font-mono text-sm bg-neural-card px-4 py-2 rounded border border-neural-cyan/20 transition-colors">
                     <Github size={16} /> Audit AI Workers on GitHub
                  </a>
               </div>
            </div>
         </div>
      </section>

      {/* Google Cloud BYOK Setup Details */}
      <section id="byok-setup" className="py-24 bg-neural-bg border-b border-gray-800">
         <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
              <div className="text-center mb-12">
                 <h2 className="text-3xl font-extrabold text-white">Host Your Own Workspace Sync</h2>
                 <p className="text-gray-400">Bring Your Own Cloud (BYOC) for ultimate control.</p>
              </div>

              <div className="bg-neural-card p-4 sm:p-8 rounded-2xl border border-gray-800 shadow-lg space-y-6">
                  <h3 className="text-2xl font-bold text-neural-cyan">OAuth Project Architecture</h3>
                  <p className="text-gray-300 text-sm leading-relaxed">
                      To ensure no centralized database has access to your files, you connect Reality directly to your Google Cloud Console project. All Google Tasks, Calendar events, and Drive files sync through your own credentials.
                  </p>
                  
                  <div className="bg-black/50 p-6 rounded-xl border border-gray-800">
                      <h4 className="text-lg font-bold text-white mb-2">Required OAuth Scopes</h4>
                      <ul className="list-disc pl-5 text-gray-400 space-y-1 text-sm font-mono">
                          <li>https://www.googleapis.com/auth/calendar.events</li>
                          <li>https://www.googleapis.com/auth/drive.file</li>
                          <li>https://www.googleapis.com/auth/tasks</li>
                          <li>https://www.googleapis.com/auth/userinfo.email</li>
                          <li>https://www.googleapis.com/auth/userinfo.profile</li>
                          <li>openid</li>
                      </ul>
                  </div>

                  <div className="space-y-6 border-l-2 border-neural-purple pl-6 mt-8">
                      <div>
                          <h4 className="text-lg font-bold text-white">1. Configure Google Cloud Console</h4>
                          <p className="text-gray-400 text-sm mt-1">
                            Go to the <a href="https://console.cloud.google.com/" target="_blank" rel="noopener noreferrer" className="text-neural-cyan hover:underline">Google Cloud Console</a>, create a new project, and enable the Calendar, Drive, and Tasks APIs.
                          </p>
                      </div>
                      <div>
                          <h4 className="text-lg font-bold text-white">2. Set Up OAuth Consent Screen</h4>
                          <p className="text-gray-400 text-sm mt-1">
                            Set up an <strong>External</strong> OAuth Consent Screen. Add your Google account email as a <strong>Test User</strong>. Add the scopes listed above.
                          </p>
                      </div>
                      <div>
                          <h4 className="text-lg font-bold text-white">3. Generate Desktop Credentials</h4>
                          <p className="text-gray-400 text-sm mt-1">
                            Go to Credentials &gt; Create Credentials &gt; OAuth client ID. Select <strong>Desktop application</strong> as the Application type, name it, and copy the generated Client ID and Secret.
                          </p>
                      </div>
                      <div>
                          <h4 className="text-lg font-bold text-white">4. Save in Reality App</h4>
                          <p className="text-gray-400 text-sm mt-1">
                            Open Reality App &gt; Go to <strong>Elite Page</strong> or <strong>Profile Page</strong> &gt; Tap the <strong>Settings icon</strong> in the top right. Paste your Client ID and Secret and click Save.
                          </p>
                      </div>
                  </div>
                  
                  <div className="mt-8 pt-8 border-t border-gray-800">
                      <h4 className="text-xl font-bold text-white mb-4">Want an AI to guide you?</h4>
                      <p className="text-gray-400 text-sm mb-4">
                          Copy this prompt and paste it into ChatGPT or Google Gemini. It will walk you through the entire Google Cloud Console setup step-by-step.
                      </p>
                      <div className="relative">
                          <div className="bg-[#0d1117] text-gray-300 text-xs font-mono p-4 rounded-xl overflow-x-auto border border-gray-800">
                              <pre className="whitespace-pre-wrap">
{`Help me set up a Google Cloud Console project for a local-first Android app called "Reality".

Walk me through these steps one by one. Don't give me the next step until I say "done".

Step 1: Go to Google Cloud Console and create a new project.
Step 2: Enable the following APIs: Google Calendar API, Google Tasks API, Google Drive API.
Step 3: Setup the "OAuth Consent Screen" as "External". Fill in the mandatory app details.
Step 4: In the "Scopes" section of the consent screen, add exactly these 6 scopes:
- https://www.googleapis.com/auth/calendar.events
- https://www.googleapis.com/auth/drive.file
- https://www.googleapis.com/auth/tasks
- https://www.googleapis.com/auth/userinfo.email
- https://www.googleapis.com/auth/userinfo.profile
- openid
Step 5: Set the publishing status to "Testing" and add my own email to the "Test Users" list.
Step 6: Go to "Credentials" -> "Create Credentials" -> "OAuth Client ID".
Step 7: Choose Application Type: "Desktop app". (This is critical because the app uses a local 127.0.0.1 redirect).
Step 8: Give me the Client ID and Client Secret.

Once I have the keys, tell me to open the Reality App -> Go to the Elite Page or Profile Page -> Tap the Settings icon in the top right -> Paste the Client ID and Secret and click Save.`}
                              </pre>
                          </div>
                          <CopyPromptButton />
                      </div>
                  </div>
              </div>
         </div>
      </section>

      {/* Quote Section */}
      <section id="quote-section" className="py-24 border-t border-gray-800 bg-black relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-transparent to-neural-cyan/5 pointer-events-none"></div>
        <div className="max-w-4xl mx-auto px-4 text-center relative z-10 space-y-6">
          <h2 className="text-3xl md:text-5xl font-bold text-white leading-tight">
            &quot;Your data. Your focus. Your life.<br/> 
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-neural-cyan to-neural-purple">On YOUR terms.</span>&quot;
          </h2>
          <p className="text-xl text-gray-400 italic">
            Built by someone who lost control of their own fingers. Designed for those who want it back.
          </p>
          <p className="text-gray-500 font-mono">— Pawan Washudev</p>
        </div>
      </section>

      {/* Visually hidden semantic AI Scraper Index Metadata */}
      <section id="ai-crawler-index" className="sr-only" aria-hidden="true" style={{ display: 'none' }}>
        <h2>Reality Technical Specification & Architecture Details</h2>
        <p>
          Reality is a secure, local-first productivity operating system and app blocker for Android. It operates strictly on-device without developer-owned database servers, upholding zero-trust architecture principles.
        </p>
        <h3>Core Engineering Specifications:</h3>
        <ul>
          <li><strong>Database ORM:</strong> Room SQLite mapping schemas locally in the app namespace.</li>
          <li><strong>On-Device Encryption:</strong> Stored locally using Android Native EncryptedSharedPreferences.</li>
          <li><strong>Google APIs Integration:</strong> Uses Google OAuth client credentials of application type Desktop Application to directly write and sync metrics to the user's personal Google Cloud workspace.</li>
          <li><strong>AI Models:</strong> Open-source GPT-OSS 20B and 120B models run on-device for complete privacy.</li>
          <li><strong>JIT Edge Cryptography:</strong> JIT encryption keys and identity parameters are generated using Cloudflare Workers edge nodes executing HMAC-SHA256 calculations locally with secret peppers.</li>
          <li><strong>Blocker Hook Loop:</strong> Leverages DeviceAdminReceiver to lock uninstalls and AccessibilityService callbacks to catch window package modifications, redirecting target distractions.</li>
          <li><strong>Assistant Engine:</strong> Runs using the Model Context Protocol (MCP) tool routing mapped to JVM registries.</li>
          <li><strong>Source Availability:</strong> 100% of code (app, website, AI workers, CI/CD, subscription management) is visible on GitHub for audit and transparency.</li>
        </ul>
      </section>

    </div>
  );
}
