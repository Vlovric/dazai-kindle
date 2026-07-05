import { useState, type ReactNode } from "react"
import { TopBarContext } from "./topBarContext"

export function TopBarProvider({ children }: { children: ReactNode }) {
    const [actions, setActions] = useState<ReactNode>(null)

    return (
        <TopBarContext.Provider value={{ actions, setActions }}>
            {children}
        </TopBarContext.Provider>
    )
}
