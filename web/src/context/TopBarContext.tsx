import { useState, type ReactNode } from "react"
import { TopBarContext } from "./topBarContext"

export function TopBarProvider({ children }: { children: ReactNode }) {
    const [searchQuery, setSearchQuery] = useState("")
    const [actions, setActions] = useState<ReactNode>(null)

    return (
        <TopBarContext.Provider value={{ searchQuery, setSearchQuery, actions, setActions }}>
            {children}
        </TopBarContext.Provider>
    )
}
