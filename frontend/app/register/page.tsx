'use client'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { Eye, EyeOff, MessageSquareCode, Loader2 } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '@/lib/api'

const schema = z.object({
  companyName: z.string().min(2, 'At least 2 characters'),
  email: z.string().email('Enter a valid email'),
  password: z.string()
    .min(8, 'At least 8 characters')
    .regex(/[A-Z]/, 'One uppercase letter required')
    .regex(/[0-9]/, 'One number required')
    .regex(/[^a-zA-Z0-9]/, 'One special character required'),
})
type FormData = z.infer<typeof schema>

const perks = ['Free forever plan', 'No credit card needed', 'Up & running in 2 min']

export default function RegisterPage() {
  const [showPw, setShowPw] = useState(false)
  const router = useRouter()
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const onSubmit = async (data: FormData) => {
    try {
      await api.post('/api/auth/register', data)
      toast.success('Account created! Sign in.')
      router.push('/login')
    } catch (err: any) {
      toast.error(err?.response?.data?.error?.code === 'EMAIL_ALREADY_EXISTS'
        ? 'This email is already registered.' : 'Something went wrong.')
    }
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--paper-2)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <div style={{ width: '100%', maxWidth: 440 }} className="animate-scale-in">

        {/* Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 9, justifyContent: 'center', marginBottom: 36 }}>
          <div style={{ width: 32, height: 32, background: 'var(--ink)', borderRadius: 9, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <MessageSquareCode size={16} color="#fff" />
          </div>
          <span style={{ fontWeight: 600, fontSize: 15, color: 'var(--ink)', letterSpacing: '-0.02em' }}>DevDocs AI</span>
        </div>

        <div className="card" style={{ padding: '36px 32px', boxShadow: '0 2px 16px rgba(0,0,0,0.06)' }}>
          <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--ink)', letterSpacing: '-0.025em', marginBottom: 6 }}>Create your account</h1>
          <p style={{ fontSize: 13, color: 'var(--ink-4)', marginBottom: 28 }}>Start free. No credit card required.</p>

          <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div>
              <label style={{ fontSize: 12, fontWeight: 500, color: 'var(--ink-3)', display: 'block', marginBottom: 6 }}>Company name</label>
              <input {...register('companyName')} placeholder="AcmePay" className="input-base" />
              {errors.companyName && <p style={{ fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.companyName.message}</p>}
            </div>

            <div>
              <label style={{ fontSize: 12, fontWeight: 500, color: 'var(--ink-3)', display: 'block', marginBottom: 6 }}>Work email</label>
              <input {...register('email')} type="email" placeholder="you@company.com" className="input-base" />
              {errors.email && <p style={{ fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.email.message}</p>}
            </div>

            <div>
              <label style={{ fontSize: 12, fontWeight: 500, color: 'var(--ink-3)', display: 'block', marginBottom: 6 }}>Password</label>
              <div style={{ position: 'relative' }}>
                <input {...register('password')} type={showPw ? 'text' : 'password'} placeholder="Min 8 chars · 1 upper · 1 number · 1 special" className="input-base" style={{ paddingRight: 42 }} />
                <button type="button" onClick={() => setShowPw(!showPw)}
                  style={{ position: 'absolute', right: 13, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--ink-4)', display: 'flex' }}>
                  {showPw ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
              {errors.password && <p style={{ fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.password.message}</p>}
            </div>

            <button type="submit" disabled={isSubmitting} className="btn-primary" style={{ width: '100%', padding: '11px', marginTop: 4, fontSize: 14 }}>
              {isSubmitting ? <><Loader2 size={14} className="animate-spin" /> Creating account...</> : 'Create account →'}
            </button>
          </form>

          {/* Perks */}
          <div style={{ display: 'flex', gap: 16, justifyContent: 'center', marginTop: 20, flexWrap: 'wrap' }}>
            {perks.map(p => (
              <span key={p} style={{ fontSize: 11, color: 'var(--ink-4)', display: 'flex', alignItems: 'center', gap: 4 }}>
                <span style={{ color: 'var(--green)' }}>✓</span> {p}
              </span>
            ))}
          </div>

          <p style={{ textAlign: 'center', fontSize: 13, color: 'var(--ink-4)', marginTop: 20 }}>
            Already have an account?{' '}
            <Link href="/login" style={{ color: 'var(--accent)', fontWeight: 500, textDecoration: 'none' }}>Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
