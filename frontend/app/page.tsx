'use client'
import Link from 'next/link'
import { ArrowRight, Zap, Shield, Code2, MessageSquareCode, CheckCircle, BarChart3 } from 'lucide-react'

const features = [
  { icon: MessageSquareCode, title: 'Agentic AI chatbot',    desc: 'The AI decides whether to search docs, fetch schemas, or generate code — not just retrieve and dump.' },
  { icon: Zap,               title: 'Streams in real-time',  desc: 'Token-by-token streaming. Developers get answers as they form, not after a loading spinner.' },
  { icon: Shield,            title: 'Tenant isolation',       desc: 'Every company\'s data lives in a completely separate namespace. Structurally impossible to cross.' },
  { icon: Code2,             title: 'One line to embed',      desc: 'A single script tag puts your chatbot on any docs page. No iframe wrestling.' },
  { icon: BarChart3,         title: 'Usage analytics',        desc: 'See which questions developers ask most. Ship better docs, not more docs.' },
]

export default function HomePage() {
  return (
    <div style={{ background: 'var(--paper)', minHeight: '100vh', fontFamily: "'DM Sans', sans-serif" }}>

      {/* Nav */}
      <nav style={{ borderBottom: '1.5px solid var(--border)', position: 'sticky', top: 0, background: 'rgba(255,255,255,0.92)', backdropFilter: 'blur(12px)', zIndex: 50 }}>
        <div style={{ maxWidth: 1080, margin: '0 auto', padding: '0 24px', height: 56, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
            <div style={{ width: 30, height: 30, background: 'var(--ink)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <MessageSquareCode size={15} color="#fff" />
            </div>
            <span style={{ fontWeight: 600, fontSize: 15, color: 'var(--ink)', letterSpacing: '-0.02em' }}>DevDocs AI</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Link href="/login"    className="btn-ghost" style={{ fontSize: 13 }}>Sign in</Link>
            <Link href="/register" className="btn-primary" style={{ fontSize: 13, padding: '7px 16px' }}>Get started free →</Link>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section style={{ maxWidth: 1080, margin: '0 auto', padding: '96px 24px 80px', textAlign: 'center' }}>
        <div className="animate-fade-up">
          <div className="badge badge-blue" style={{ marginBottom: 24, fontSize: 12 }}>
            <span style={{ width: 6, height: 6, background: 'var(--accent)', borderRadius: '50%', display: 'inline-block' }} />
            Now in beta — free for early teams
          </div>
        </div>

        <h1 className="animate-fade-up delay-50" style={{ fontSize: 'clamp(36px,5vw,58px)', fontWeight: 700, lineHeight: 1.08, letterSpacing: '-0.035em', color: 'var(--ink)', marginBottom: 20, maxWidth: 700, margin: '0 auto 20px' }}>
          Your API docs,<br />
          <span style={{ color: 'var(--accent)' }}>understood instantly.</span>
        </h1>

        <p className="animate-fade-up delay-100" style={{ fontSize: 17, color: 'var(--ink-3)', maxWidth: 520, margin: '0 auto 36px', lineHeight: 1.65, fontWeight: 400 }}>
          Upload your OpenAPI spec. Get an AI that answers developer questions with exact answers and code examples — embedded on your docs in 30 seconds.
        </p>

        <div className="animate-fade-up delay-150" style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link href="/register" className="btn-primary" style={{ fontSize: 14, padding: '11px 22px' }}>
            Start for free <ArrowRight size={15} />
          </Link>
          <Link href="/login" className="btn-secondary" style={{ fontSize: 14, padding: '11px 22px' }}>
            Sign in
          </Link>
        </div>

        {/* Social proof strip */}
        <div className="animate-fade-up delay-200" style={{ marginTop: 36, display: 'flex', gap: 24, justifyContent: 'center', flexWrap: 'wrap' }}>
          {['No credit card', 'Free plan forever', 'OpenAPI 3.0 support'].map(t => (
            <span key={t} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: 'var(--ink-4)' }}>
              <CheckCircle size={13} color="var(--green)" /> {t}
            </span>
          ))}
        </div>
      </section>

      {/* Chat demo */}
      <section style={{ maxWidth: 680, margin: '0 auto', padding: '0 24px 96px' }}>
        <div className="card animate-scale-in delay-200" style={{ overflow: 'hidden', boxShadow: '0 8px 40px rgba(0,0,0,0.08)' }}>
          {/* Window chrome */}
          <div style={{ background: 'var(--paper-2)', borderBottom: '1.5px solid var(--border)', padding: '12px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ display: 'flex', gap: 6 }}>
              {['#ff5f57','#febc2e','#28c840'].map(c => <div key={c} style={{ width: 11, height: 11, background: c, borderRadius: '50%' }} />)}
            </div>
            <div style={{ flex: 1, background: 'var(--border)', borderRadius: 6, height: 22, display: 'flex', alignItems: 'center', paddingLeft: 10 }}>
              <span style={{ fontSize: 11, color: 'var(--ink-4)' }}>docs.acmepay.com — API Assistant</span>
            </div>
          </div>
          {/* Messages */}
          <div style={{ padding: '24px 20px', display: 'flex', flexDirection: 'column', gap: 20, background: '#fff' }}>
            {/* User */}
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <div style={{ background: 'var(--ink)', color: '#fff', borderRadius: '14px 14px 4px 14px', padding: '10px 14px', fontSize: 13, maxWidth: 280, lineHeight: 1.5 }}>
                How do I authenticate with your API?
              </div>
            </div>
            {/* AI thinking */}
            <div style={{ display: 'flex', gap: 10 }}>
              <div style={{ width: 28, height: 28, background: 'var(--accent-bg)', border: '1.5px solid #bfdbfe', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                <MessageSquareCode size={13} color="var(--accent)" />
              </div>
              <div style={{ flex: 1 }}>
                <div className="badge badge-gray" style={{ marginBottom: 10, fontSize: 11 }}>
                  🔍 Searching docs...
                </div>
                <p style={{ fontSize: 13, color: 'var(--ink-2)', lineHeight: 1.65, margin: '0 0 10px' }}>
                  AcmePay uses <strong>Bearer token authentication</strong>. Add your API key to the Authorization header on every request:
                </p>
                <div style={{ background: 'var(--paper-2)', border: '1.5px solid var(--border)', borderRadius: 10, padding: '12px 14px', fontFamily: "'DM Mono', monospace", fontSize: 12, color: 'var(--ink-2)', lineHeight: 1.8 }}>
                  <span style={{ color: 'var(--ink-4)' }}>// Set this header on all requests</span><br />
                  Authorization: Bearer {'<YOUR_API_KEY>'}<br />
                  Content-Type: application/json
                </div>
                <p style={{ fontSize: 11, color: 'var(--ink-4)', marginTop: 8 }}>📎 Source: POST /v1/auth/token</p>
              </div>
            </div>
          </div>
          {/* Input bar */}
          <div style={{ borderTop: '1.5px solid var(--border)', padding: '12px 16px', display: 'flex', gap: 10, alignItems: 'center', background: '#fff' }}>
            <div style={{ flex: 1, background: 'var(--paper-2)', border: '1.5px solid var(--border)', borderRadius: 9, padding: '9px 14px', fontSize: 13, color: 'var(--ink-4)' }}>
              Ask anything about this API...
            </div>
            <div className="btn-primary" style={{ padding: '9px 14px', cursor: 'default' }}>
              <ArrowRight size={14} />
            </div>
          </div>
        </div>
      </section>

      {/* Features grid */}
      <section style={{ background: 'var(--paper-2)', borderTop: '1.5px solid var(--border)', borderBottom: '1.5px solid var(--border)', padding: '80px 24px' }}>
        <div style={{ maxWidth: 1080, margin: '0 auto' }}>
          <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--ink-4)', letterSpacing: '0.08em', textTransform: 'uppercase', textAlign: 'center', marginBottom: 12 }}>Everything you need</p>
          <h2 style={{ fontSize: 32, fontWeight: 700, letterSpacing: '-0.03em', textAlign: 'center', marginBottom: 48, color: 'var(--ink)' }}>Built for developer teams</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
            {features.map((f, i) => (
              <div key={f.title} className={`card card-hover animate-fade-up delay-${i * 50}`} style={{ padding: '24px' }}>
                <div style={{ width: 38, height: 38, background: 'var(--paper-3)', border: '1.5px solid var(--border)', borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
                  <f.icon size={17} color="var(--ink-2)" />
                </div>
                <h3 style={{ fontSize: 14, fontWeight: 600, color: 'var(--ink)', marginBottom: 6, letterSpacing: '-0.01em' }}>{f.title}</h3>
                <p style={{ fontSize: 13, color: 'var(--ink-3)', lineHeight: 1.6 }}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section style={{ maxWidth: 1080, margin: '0 auto', padding: '96px 24px', textAlign: 'center' }}>
        <h2 className="animate-fade-up" style={{ fontSize: 36, fontWeight: 700, letterSpacing: '-0.03em', marginBottom: 16, color: 'var(--ink)' }}>Ready to ship better docs?</h2>
        <p className="animate-fade-up delay-50" style={{ fontSize: 16, color: 'var(--ink-3)', marginBottom: 32 }}>Upload your first spec in under 2 minutes.</p>
        <Link href="/register" className="btn-primary animate-fade-up delay-100" style={{ fontSize: 15, padding: '12px 28px' }}>
          Get started free <ArrowRight size={15} />
        </Link>
      </section>

      {/* Footer */}
      <footer style={{ borderTop: '1.5px solid var(--border)', padding: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', maxWidth: 1080, margin: '0 auto' }}>
        <span style={{ fontSize: 13, color: 'var(--ink-4)' }}>© 2026 DevDocs AI · Built by Anas Khan</span>
        <a href="https://github.com/YOUR_USERNAME/devdocs-ai" style={{ fontSize: 13, color: 'var(--ink-4)', textDecoration: 'none' }} className="btn-ghost">GitHub ↗</a>
      </footer>
    </div>
  )
}
