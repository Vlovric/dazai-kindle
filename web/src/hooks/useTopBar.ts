import { useContext } from "react"
import { TopBarContext } from "../context/topBarContext"

export function useTopBar() {
    const context = useContext(TopBarContext)
    if (!context) {
        throw new Error("useTopBar must be used within a TopBarProvider")
    }
    return context
}
