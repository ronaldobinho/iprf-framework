import { Demo } from "@/components/landing/Demo";
import { BudgetNote, LayerDiagram } from "@/components/landing/LayerDiagram";
import {
  Assessment,
  Contact,
  Hero,
  MethodologyTeaser,
  Problem,
  Refusals,
  ReproducibleResult,
} from "@/components/landing/sections";
import { Section } from "@/components/ui";
import { getAllDocs } from "@/lib/docs";

export default function Home() {
  const docs = getAllDocs();

  return (
    <>
      <Hero />
      <Problem />

      <Section
        eyebrow="The architecture"
        title="Decide before the transaction arrives what can be evaluated in-path"
        lede="Everything else in the framework is downstream of that sentence. The failure it prevents is specific: a control that is correct in isolation but, placed on the authorization path, performs a query. Under normal load nobody notices. Under the load where it matters, the institution starts failing legitimate payments at exactly the moment it most needs to be working."
      >
        <LayerDiagram />
        <BudgetNote />
      </Section>

      <Section
        id="demo"
        eyebrow="Live, in your browser"
        title="Watch it decide, and see why"
        lede="The engine, ported to TypeScript and running client-side with no backend. Change the amount, the destination, the device or the hour and the decision recomputes — with every rule that fired, its version, and its individual contribution to the score."
      >
        <Demo />
      </Section>

      <ReproducibleResult />
      <Refusals />
      <Assessment />
      <MethodologyTeaser docs={docs} />
      <Contact />
    </>
  );
}
