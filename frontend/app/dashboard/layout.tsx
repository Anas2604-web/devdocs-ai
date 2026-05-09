'use client'
import { useEffect } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import Link from 'next/link'
import { MessageSquareCode, FileText, LayoutDashboard, LogOut, Zap } from 'lucide-react'
import { useAuthStore } from '@/lib/authStore'
import toast from 'react-hot-toast'

const nav = [
  { href: '/dashboard',       icon: LayoutDashboard,  label: 'Overview' },
  { href: '/dashboard/specs', icon: FileText,          label: 'API Specs' },
  { href: '/dashboard/chat',  icon: MessageSquareCode, label: 'Chat' },
]

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, clearAuth } = useAuthStore()
  const router = useRouter()
  const path = usePathname()

  useEffect(() => { if (!isAuthenticated) router.replace('/login') }, [isAuthenticated, router])
  if (!isAuthenticated) return null

  return (
    <div style={{ minHeight: '100vh', display: 'flex', background: 'var(--paper-2)', fontFamily: "'DM Sans', sans-serif" }}>

      {/* Sidebar */}
      <aside style={{ width: 220, background: 'var(--paper)', borderRight: '1.5px solid var(--border)', display: 'flex', flexDirection: 'column', padding: '20px 12px', position: 'fixed', height: '100vh' }}>

        {/* Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '4px 8px', marginBottom: 32 }}>
          <div style={{ width: 28, height: 28, background: 'var(--ink)', borderRadius: 7, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <MessageSquareCode size={13} color="#fff" />
          </div>
          <span style={{ fontWeight: 600, fontSize: 14, color: 'var(--ink)', letterSpacing: '-0.02em' }}>DevDocs AI</span>
        </div>

        {/* Nav items */}
        <nav style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {nav.map(({ href, icon: Icon, label }) => {
            const active = path === href || (href !== '/dashboard' && path.startsWith(href))
            return (
              <Link key={href} href={href} style={{
                display: 'flex', alignItems: 'center', gap: 9,
                padding: '8px 10px', borderRadius: 8, textDecoration: 'none', fontSize: 13, fontWeight: active ? 500 : 400,
                color: active ? 'var(--ink)' : 'var(--ink-3)',
                background: active ? 'var(--paper-2)' : 'transparent',
                transition: 'all 0.12s',
              }}
              onMouseEnter={e => { if (!active) (e.currentTarget as HTMLElement).style.background = 'var(--paper-2)' }}
              onMouseLeave={e => { if (!active) (e.currentTarget as HTMLElement).style.background = 'transparent' }}>
                <Icon size={14} color={active ? 'var(--ink)' : 'var(--ink-4)'} />
                {label}
              </Link>
            )
          })}
        </nav>

        {/* Plan badge */}
        <div style={{ margin: '16px 0', padding: '12px', background: 'var(--accent-bg)', border: '1.5px solid #bfdbfe', borderRadius: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
            <Zap size={12} color="var(--accent)" />
            <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--accent)' }}>Free plan</span>
          </div>
          <p style={{ fontSize: 11, color: 'var(--ink-4)', margin: 0 }}>500 questions / month</p>
        </div>

        {/* Logout */}
        <button onClick={() => { clearAuth(); toast.success('Signed out'); router.push('/login') }}
          style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '8px 10px', borderRadius: 8, border: 'none', background: 'transparent', cursor: 'pointer', fontSize: 13, color: 'var(--ink-4)', width: '100%', transition: 'all 0.12s' }}
          onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = 'var(--paper-2)'; (e.currentTarget as HTMLElement).style.color = 'var(--ink)' }}
          onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent'; (e.currentTarget as HTMLElement).style.color = 'var(--ink-4)' }}>
          <LogOut size={14} /> Sign out
        </button>
      </aside>

      {/* Main content */}
      <main style={{ marginLeft: 220, flex: 1, minHeight: '100vh' }} className="animate-fade-in">
        {children}
      </main>
    </div>
  )
}
