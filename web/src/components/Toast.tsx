import { useEffect } from "react"

export function Toast({ message, onDismiss, duration = 3000 }: {
    message: string
    onDismiss: () => void
    duration?: number
}) {
    useEffect(() => {
        const timer = setTimeout(onDismiss, duration)
        return () => clearTimeout(timer)
    }, [message, duration, onDismiss])

    return (
        <div className="fixed bottom-lg right-lg bg-success text-on-success rounded-lg px-md py-sm shadow-lg flex items-center gap-sm">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5 shrink-0">
                <path fillRule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z" clipRule="evenodd" />
            </svg>
            <span className="text-label-md font-body">{message}</span>
        </div>
    )
}
