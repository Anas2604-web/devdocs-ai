'use client'
import Link from 'next/link'
import { FileText, MessageSquareCode, Zap, ArrowRight, Upload } from 'lucide-react'

const stats = [
  { label: 'API Specs',         value: '0', sub: 'uploaded',       icon: FileText,          color: '#2563eb', bg: '#eff6ff', border: '#bfdbfe' },
  { label: 'Questions',         value: '0', sub: 'answered',        icon: MessageSquareCode, color: '#7c3aed', bg: '#f5f3ff', border: '#ddd6fe' },
  { label: 'Cache hit rate',    value: '—', sub: 'this month',      icon: Zap,               color: '#d97706', bg: '#fffbeb', border: '#fde68a' },
]

export default function DashboardPage() {
  return (
    <div style={{ padding: '40px 40px', maxWidth: 900 }}>

      {/* Header */}
      <div className="animate-fade-up" style={{ marginBottom: 32 }}>
        <h1 style={{ fontSize: 22, fontWeight: 700, color: 'var(--ink)', letterSpacing: '-0.025em', marginBottom: 4 }}>Overview</h1>
        <p style={{ fontSize: 13, color: 'var(--ink-4)' }}>Welcome to your DevDocs AI workspace.</p>
      </div>

      {/* Stats row */}
      <div className="animate-fade-up delay-50" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14, marginBottom: 32 }}>
        {stats.map(s => (
          <div key={s.label} className="card" style={{ padding: '20px 22px' }}>
            <div style={{ width: 34, height: 34, background: s.bg, border: `1.5px solid ${s.border}`, borderRadius: 9, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
              <s.icon size={15} color={s.color} />
            </div>
            <div style={{ fontSize: 26, fontWeight: 700, color: 'var(--ink)', letterSpacing: '-0.03em', lineHeight: 1 }}>{s.value}</div>
            <div style={{ fontSize: 12, color: 'var(--ink-4)', marginTop: 5 }}>{s.label} <span style={{ color: 'var(--ink-4)' }}>{s.sub}</span></div>
          </div>
        ))}
      </div>

      {/* Getting started */}
      <div className="animate-fade-up delay-100">
        <p style={{ fontSize: 11, fontWeight: 600, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.07em', marginBottom: 12 }}>Get started</p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {/* Step 1 - active */}
          <div className="card" style={{ padding: '20px 22px', display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{ width: 38, height: 38, background: 'var(--ink)', borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <Upload size={16} color="#fff" />
            </div>
            <div style={{ flex: 1 }}>
              <p style={{ fontSize: 14, fontWeight: 600, color: 'var(--ink)', marginBottom: 3 }}>Upload your first API spec</p>
              <p style={{ fontSize: 13, color: 'var(--ink-4)' }}>Upload an OpenAPI 3.0 YAML or JSON file to get started.</p>
            </div>
            <Link href="/dashboard/specs" className="btn-primary" style={{ fontSize: 13, padding: '8px 16px', flexShrink: 0 }}>
              Upload <ArrowRight size={13} />
            </Link>
          </div>

          {/* Step 2 - locked */}
          <div className="card" style={{ padding: '20px 22px', display: 'flex', alignItems: 'center', gap: 16, opacity: 0.45 }}>
            <div style={{ width: 38, height: 38, background: 'var(--paper-3)', border: '1.5px solid var(--border)', borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <MessageSquareCode size={16} color="var(--ink-4)" />
            </div>
            <div style={{ flex: 1 }}>
              <p style={{ fontSize: 14, fontWeight: 600, color: 'var(--ink)', marginBottom: 3 }}>Chat with your API</p>
              <p style={{ fontSize: 13, color: 'var(--ink-4)' }}>Upload a spec first to unlock the AI chatbot.</p>
            </div>
            <span style={{ fontSize: 12, color: 'var(--ink-4)', background: 'var(--paper-3)', padding: '6px 12px', borderRadius: 7, border: '1.5px solid var(--border)', flexShrink: 0 }}>Locked</span>
          </div>
        </div>
      </div>
    </div>
  )
}
