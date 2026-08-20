/**
 * Design reminder — Chronographic Modernism:
 * Keep app framing quiet and technical. The page itself owns the editorial hierarchy;
 * global providers should never add decorative UI or compete with the time display.
 */
import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import ErrorBoundary from "./components/ErrorBoundary";
import Home from "./pages/Home";

export default function App() {
  return (
    <ErrorBoundary>
      <TooltipProvider>
        <Toaster position="top-center" />
        <Home />
      </TooltipProvider>
    </ErrorBoundary>
  );
}
