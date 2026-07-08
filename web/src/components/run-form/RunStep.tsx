import type { ReactNode } from "react"

export function RunStep({ step, label, children }: {
    step: number
    label: string
    children: ReactNode
}) {
    return (
        <div className="mb-xl">
            <div className="flex items-center gap-sm mb-sm">
                <span className="flex items-center justify-center w-6 h-6 rounded-full border border-outline text-label-sm font-body text-primary shrink-0">
                    {step}
                </span>
                <h3 className="font-body text-body-lg text-on-surface">{label}</h3>
            </div>
            {children}
        </div>
    )
}
