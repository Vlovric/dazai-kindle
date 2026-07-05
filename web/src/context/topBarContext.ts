import { createContext, type ReactNode } from "react"

export interface TopBarContextValue {
    actions: ReactNode
    setActions: (actions: ReactNode) => void
}

export const TopBarContext = createContext<TopBarContextValue | null>(null)
