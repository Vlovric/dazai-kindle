import type { ReactNode } from "react"

export function Modal({ title, onClose, children, footer }: {
    title: string
    onClose: () => void
    children: ReactNode
    footer?: ReactNode
}) {
    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/40 px-md"
            onClick={onClose}
        >
            <div
                className="w-full max-w-[480px] bg-surface-container-lowest border border-outline-variant rounded-lg shadow-lg flex flex-col max-h-[80vh]"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="flex items-center justify-between px-lg py-md border-b border-outline-variant">
                    <h3 className="font-headline text-headline-md text-primary">{title}</h3>
                    <button
                        type="button"
                        onClick={onClose}
                        aria-label="Close"
                        className="text-secondary hover:text-primary text-label-md font-body"
                    >
                        ✕
                    </button>
                </div>
                <div className="flex-1 overflow-y-auto px-lg py-md">
                    {children}
                </div>
                {footer && (
                    <div className="flex items-center justify-end gap-md px-lg py-md border-t border-outline-variant">
                        {footer}
                    </div>
                )}
            </div>
        </div>
    )
}
