import { Hero } from "@/components/landing/Hero";
import { Journey } from "@/components/landing/Journey";
import {
  FinalCta,
  Integration,
  JourneyIntro,
  Performance,
  ScrollInvitation,
} from "@/components/landing/sections";

/**
 * The landing is one argument delivered in order: what IPRF is, then the
 * transaction walked through every layer that evaluates it, then what the
 * framework is built to and what it refuses to claim, then how to get it.
 */
export default function Home() {
  return (
    <>
      <Hero />
      <ScrollInvitation />
      <JourneyIntro />
      <Journey />
      <Performance />
      <Integration />
      <FinalCta />
    </>
  );
}
