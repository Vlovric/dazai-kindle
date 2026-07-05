import { createContext, type ReactNode } from "react"

export interface TopBarContextValue {
    searchQuery: string
    setSearchQuery: (query: string) => void
    actions: ReactNode
    setActions: (actions: ReactNode) => void
}

export const TopBarContext = createContext<TopBarContextValue | null>(null)
