import { Link } from 'react-router'

export function BackToConsultations() {
  return (
    <Link to="/admin/consultations" className="inline-block text-sm underline">
      Back to the consultations
    </Link>
  )
}
