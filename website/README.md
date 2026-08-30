# Reality Website

Official website and ecosystem dashboard for the Reality App.

**Live at: [https://reality.neubofy.in](https://reality.neubofy.in)**

---

## 📋 Overview

This is a **Next.js 15+** project that hosts:
- **Home Dashboard** - App showcase and feature overview
- **Tapashya Web Timer** - Cross-platform deep focus timer with QR sync to Android app
- **Privacy Policy** - Transparent data handling and architecture details
- **Terms of Service** - Legal framework for source-available licensing
- **Elite Membership** - Subscription and premium features information
- **README Page** - Comprehensive app documentation

All pages are **100% source-available**, auditable, and deployed on Vercel.

---

## 🚀 Getting Started

### Prerequisites
- **Node.js 18+** (LTS recommended)
- **npm**, **yarn**, **pnpm**, or **bun**

### Installation

```bash
# Clone the repository
git clone https://github.com/neubofy/Reality.git
cd Reality/website

# Install dependencies
npm install
# or
yarn install
# or
pnpm install
# or
bun install
```

### Development Server

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser to see the result.

You can start editing pages by modifying `src/app/page.tsx` or other route files. The page auto-updates as you make changes thanks to Next.js Fast Refresh.

---

## 📁 Project Structure

```
website/
├── src/
│   ├── app/
│   │   ├── page.tsx                 # Home dashboard
│   │   ├── layout.tsx               # Root layout with fonts and metadata
│   │   ├── globals.css              # Global styles
│   │   ├── favicon.ico              # Site icon
│   │   │
│   │   ├── tapashya/
│   │   │   └── page.tsx             # Deep focus timer (web version)
│   │   │
│   │   ├── privacypolicy/
│   │   │   └── page.tsx             # Privacy policy and data architecture
│   │   │
│   │   ├── termsofservice/
│   │   │   └── page.tsx             # Terms of service and licensing
│   │   │
│   │   ├── promembers/
│   │   │   └── page.tsx             # Elite membership information
│   │   │
│   │   ├── readme/
│   │   │   └── page.tsx             # Comprehensive app README
│   │   │
│   │   └── api/
│   │       ├── auth/
│   │       │   ├── google/
│   │       │   │   ├── login/route.ts      # Google OAuth login flow
│   │       │   │   └── callback/route.ts   # OAuth callback handler
│   │       │   └── logout/route.ts         # Clear auth session
│   │       │
│   │       └── calendar/
│   │           └── events/route.ts         # Fetch calendar events for Tapashya
│   │
│   ├── lib/                         # Utility functions and helpers
│   │   └── [utilities]
│   │
│   └── Components/
│       ├── HeroActions.tsx          # CTA buttons and downloads
│       ├── ScreenshotGallery.tsx    # App screenshot carousel
│       ├── EcosystemAnimation.tsx   # Animated feature showcase
│       ├── ArchitectureBlueprints.tsx # Technical architecture visualization
│       ├── CopyPromptButton.tsx     # Copy-to-clipboard functionality
│       └── MobileNav.tsx            # Mobile navigation menu
│
├── public/                          # Static assets
│   └── dashboard_mockup.png         # Phone mockup image
│
├── package.json                     # Dependencies and scripts
├── tsconfig.json                    # TypeScript configuration
├── next.config.mjs                  # Next.js configuration
├── tailwind.config.js               # Tailwind CSS configuration
├── postcss.config.mjs               # PostCSS configuration
├── eslint.config.mjs                # ESLint configuration
└── vercel.json                      # Vercel deployment config
```

---

## 🎨 Key Pages

### 1. **Home Page** (`/`)
- Hero section with app highlights
- Feature showcase and screenshots
- Architecture blueprints
- BYOC (Bring Your Own Cloud) setup guide
- Ecosystem animation
- Quote and call-to-action

### 2. **Tapashya Web Timer** (`/tapashya`)
- Full-featured deep focus timer
- Calendar integration (Google OAuth)
- Session history and analytics
- QR code export for Android sync
- Mini/Picture-in-Picture mode
- Local storage persistence

### 3. **Privacy Policy** (`/privacypolicy`)
- Comprehensive data privacy details
- Local-first architecture explanation
- Cloudflare Workers and JIT cryptography info
- Google Workspace API disclosures
- Data retention and deletion policies
- Contact information

### 4. **Terms of Service** (`/termsofservice`)
- Source-available licensing terms
- System permissions disclaimer
- Data sync responsibility
- Liability limitations
- Google API usage policies
- License auditing contact info

### 5. **Elite Membership** (`/promembers`)
- Premium features overview
- Subscription benefits
- Pricing and billing information
- Feature comparison

### 6. **README** (`/readme`)
- Full app documentation
- Setup instructions
- Feature deep-dives
- Technical specifications
- Support and contact links

---

## 🔧 Technology Stack

| Component | Technology |
| :--- | :--- |
| **Framework** | Next.js 15+ with App Router |
| **Language** | TypeScript |
| **Styling** | Tailwind CSS + PostCSS |
| **UI Components** | React + Lucide Icons |
| **State Management** | React Hooks |
| **Data Fetching** | Fetch API + Server Components |
| **Authentication** | Google OAuth 2.0 |
| **QR Generation** | qrcode.react |
| **Deployment** | Vercel |
| **Icons** | Lucide React |

---

## 🔐 API Routes

### Google Authentication
- **`GET /api/auth/google/login`** - Initiate Google OAuth flow
- **`GET /api/auth/google/callback`** - Handle OAuth callback and set secure cookies
- **`POST /api/auth/logout`** - Clear authentication session

### Calendar Integration
- **`GET /api/calendar/events`** - Fetch user's Google Calendar events (authenticated)

All API routes use **secure HTTP-only cookies** for authentication—no client-side token storage.

---

## 🌐 Deployment

### Deploy to Vercel (Recommended)

The easiest way to deploy the website is using the [Vercel Platform](https://vercel.com).

1. Push your code to GitHub
2. Connect your GitHub repo to Vercel
3. Vercel automatically detects Next.js and configures the build
4. Deploy with one click

**Custom Domain Setup:**
1. Point your domain's DNS to Vercel
2. Add custom domain in Vercel project settings
3. Automatic SSL certificate provisioning

**Environment Variables:**
```env
# Google OAuth (for calendar integration)
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
GOOGLE_REDIRECT_URI=https://reality.neubofy.in/api/auth/google/callback

# Session security
SESSION_SECRET=your_secure_random_string
```

---

## 📦 Build & Production

### Build for Production

```bash
npm run build
# or
yarn build
# or
pnpm build
# or
bun build
```

Generates an optimized production build in the `.next` directory.

### Start Production Server

```bash
npm start
# or
yarn start
# or
pnpm start
# or
bun start
```

---

## 🧪 Development Workflow

### Code Style & Linting

```bash
npm run lint
# or
yarn lint
```

ESLint configuration enforces code quality and consistency.

### TypeScript Checking

```bash
npm run type-check
```

---

## 📱 Responsive Design

- **Mobile-first** approach with Tailwind CSS
- **Fully responsive** across all screen sizes
- **Dark mode** by default (AMOLED-optimized)
- **Fast performance** with image optimization
- **Accessible** with semantic HTML and ARIA labels

---

## 🔗 Key Integration Points

### Android App Integration
- **QR Code Sync**: Tapashya timer sessions export as QR codes for scanning in Reality App
- **Deep Links**: Custom URI scheme `Reality:Tapashya?data=...` for direct import
- **Local Storage**: All session data persists in browser localStorage

### Google Cloud Integration
- **OAuth 2.0**: Secure authentication via your own Google Cloud project
- **Calendar API**: Real-time event fetching for Tapashya scheduling
- **Tasks API**: Task synchronization (coming soon)

---

## 🚀 Features

✅ **100% Source-Available** - All code auditable on GitHub  
✅ **Server-Side Rendering** - Fast initial page load  
✅ **Static Generation** - Pre-render pages at build time  
✅ **Image Optimization** - Automatic Next.js image optimization  
✅ **Font Optimization** - Custom fonts via `next/font`  
✅ **SEO Optimized** - Structured data and meta tags  
✅ **Dark Mode** - AMOLED-optimized styling  
✅ **API Routes** - Backend logic in `/api` folder  
✅ **Authentication** - Secure Google OAuth flow  

---

## 📚 Learn More

- **[Next.js Documentation](https://nextjs.org/docs)** - Learn about Next.js features and API
- **[Learn Next.js](https://nextjs.org/learn)** - Interactive Next.js tutorial
- **[Tailwind CSS Docs](https://tailwindcss.com/docs)** - Styling and components
- **[Vercel Documentation](https://vercel.com/docs)** - Deployment and edge functions

---

## 🤝 Contributing

This website is **source-available for transparency**. We value:
- Security audit reports
- Bug reports and feature suggestions
- Documentation improvements
- Design and UX feedback

---

## 📄 License

Source-available for audit and personal study. Unauthorized redistribution prohibited.

See [Terms of Service](https://reality.neubofy.in/termsofservice) for complete details.

---

## 📞 Support

- **Website**: [reality.neubofy.in](https://reality.neubofy.in)
- **Privacy Policy**: [reality.neubofy.in/privacypolicy](https://reality.neubofy.in/privacypolicy)
- **Terms of Service**: [reality.neubofy.in/termsofservice](https://reality.neubofy.in/termsofservice)
- **Email**: [support@neubofy.in](mailto:support@neubofy.in)
- **GitHub Issues**: [Report Issues](https://github.com/neubofy/Reality/issues)

---

**Made with ❤️ by Pawan Washudev @ Neubofy**
