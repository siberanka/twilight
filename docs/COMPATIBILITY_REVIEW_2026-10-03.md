# Compatibility review and acceptance expansion - 3 October 2026

Twilight's target is automatic Java/Bedrock content parity with unchanged Java
assets. The BedrockGen review broadens the checklist; it does not establish that
Twilight already implements the compared features. The current evidence remains
the locally tested `30511e9` snapshot, with 87 tests and the published client
comparisons. Complete interactive UI and animation parity remain open.

Two additional compiler regressions now pass: namespace-qualified glyph images
with identical filenames remain distinct, and multi-row bitmap sheets preserve
their cells while ignoring blank code points. The local full build passes
**89 tests in 19 suites**; the product JAR is byte-identical to the baseline.
[Validation results](acceptance/validation-2026-10-03.json) also record rejection
of incomplete acceptance and an attempted evidence-free status promotion.

## External material reviewed

The [BBB listing](https://builtbybit.com/resources/bedrockgen-java-to-bedrock-converter.115920/)
identifies v3 / 1.0.349. The user-supplied menu/tooltip panel distinguishes custom
chest artwork from framed hover text that requires the HUD add-on. The listing
marks the software closed-source. No binary, code or artwork was imported.

The [official wiki](https://docs.bedrockgen.com/) documents source integrations,
item geometry/poses/components, blocks, equipment, furniture, models, glyphs,
sounds, animated textures, chest menus, scoreboards, skulls and delivery.
Its runtime is split across converter, server integration and Geyser extension.
The HUD add-on covers BetterHud, MythicHUD, CustomNameplates, LuxDialogues and
CustomFishing. Menu import uses configuration or live inventory measurements.
Glyph tools include review, overrides, page protection and optional source
rewriting. Documented limits include some model rotations, mob armor, paintings,
layered menus and client-specific presentation. The settings and dedicated
elytra sections disagree about gliding; defaults also differ between sections.
These are published claims, not independently measured competitor results.

Read coverage: all 17 English wiki sections, including installation variants,
commands/settings, supported/unsupported features and emoji troubleshooting;
the BBB overview and
[dependencies](https://builtbybit.com/resources/bedrockgen-java-to-bedrock-converter.115920/field?field=dependencies);
all four update-list pages. Japanese/Chinese translations were not treated as
additional feature specifications. The legacy wiki host and full BBB attachment
images were inaccessible through the available reader. The supplied screenshot
was reviewed directly; video behavior and a running competitor build were not
verified. This is not a claim to have inspected unavailable material.

The [latest updates](https://builtbybit.com/resources/bedrockgen-java-to-bedrock-converter.115920/updates)
mention duplicate GUI filenames, inventory-versus-held display contexts,
wide ranks, model seats/rotation, tooltips and connection recovery.
[Page 2](https://builtbybit.com/resources/bedrockgen-java-to-bedrock-converter.115920/updates?page=2)
adds useful regression triggers around false slot-grid detection, code-point
collisions, phone paths, memory and source protection.
[Page 3](https://builtbybit.com/resources/bedrockgen-java-to-bedrock-converter.115920/updates?page=3)
records armor, item identity, model animation timing and integrated-pack issues.
[Page 4](https://builtbybit.com/resources/bedrockgen-java-to-bedrock-converter.115920/updates?page=4)
records early furniture, bow and GUI conversion changes. Historical fixes and
current marketing claims are not substitutes for reproducing a test ourselves.

## Twilight's own acceptance contract

The machine-readable [catalog](acceptance/catalog.json) defines **40 areas and
289 required scenarios**. These are test obligations, not 289 passing tests.
At this checkpoint, 21 areas have partial implementation/evidence, 18 are
planned, and one records a known blocking UI failure. No entire area is marked
accepted. A valid catalog is not a successful conversion or runtime test.

Each scenario requires six distinct stages: structural validation, unchanged
Java reference, Bedrock visual comparison, runtime behavior, reload/reconnect,
and source integrity. Accepted records must identify the reviewed build,
Java/Bedrock/Geyser versions, reviewer and hashed evidence. Missing, stale or
changed evidence cannot establish completion. The local completeness gate
currently rejects the catalog as expected.

| Area | Current Twilight boundary | Required next evidence |
| --- | --- | --- |
| Source discovery | Standard packs and several provider APIs; recognition is not complete integration. | Every configured source, priority conflict, missing registry and reload sequence; EcoItems remains unverified. |
| Item visuals | Static geometry, composites and model-based GUI icons implemented; poses only sampled. | Every display context, both hands, mirrors, thin planes, alpha/depth and context-dependent definitions. |
| Item behavior | Selected mapping predicates exist. | Weapon state transitions, consumption, tools, cooldowns, repairs, recipes and creative inventory. |
| Texture animation | First authored frame exports. | Frame ordering, nonuniform duration, interpolation and uninterrupted playback in every display context. |
| Equipment | Complete equipment conversion is not accepted. | Armor layers and joints, dyes/glint, elytra transitions, cosmetics and rig coexistence. |
| Blocks | Discovery exists; custom block parity is not implemented. | Every state and orientation, collision, placement, break speed, drops, sounds and reload. |
| Furniture and models | Development display bridge and sampled provider evidence. | Full animation/state sequences, independent head movement, seats, hitboxes, late observers and cleanup. |
| Glyphs | Measured sizes/baselines and wider atlas cells work for tested samples. | Fractional detail, bearings, named-font collisions and all actual chat/name/menu/HUD contexts. |
| Menus | Large images render in chat; actual title clipping is a recorded failure. | Layout composition plus correct slots, hover, clicks, permissions and dynamic contents. |
| Tooltips | No full custom-tooltip adapter. | Authored frame, tint, line breaks, text metrics, edge wrapping and pointer/controller focus. |
| HUD and scoreboard | Native glyph export does not provide a complete screen adapter. | Live variables, images, timed layers, dialogue choices, fishing states and reconnect cleanup. |
| World, audio and skulls | Sound compiler has structural tests; other systems remain partial/planned. | Playback, profile registration, mob equipment, paintings, seasons and waypoint presentation. |
| Deployment | Local transactional deployment and rollback implemented. | Proxy/standalone, multi-backend state, interrupted transfers, bounded retries and cache recovery. |
| Resources and devices | Windows snapshots available; large atlas memory is documented. | Measured heap/GPU cost, join latency and tick impact; Android/iOS, controller and constrained hardware. |

Our requirements exceed a single showcase image: no authored-size shrink-to-fit,
no Java-source rewrites, arbitrary image-layer sequences, context-aware fonts,
repeatable output, explicit omissions and evidence for actual interactions.
These are goals to implement and verify, not claims of superiority today.

## Real menu material inventory

A new read-only scan found **42 DeluxeMenus definitions** in the available test
sources: 17 Box and 25 Survival. Sizes include 9, 27, 36, 45 and 54 slots;
one definition declares 53. Eight titles contain image placeholders and one
uses escaped Unicode. Forty definitions include lore, 21 include conditional
entries, and all 42 include actions. These are syntax-level counts, not resolved
YAML/layout or visual acceptance. The unavailable older Survival directory was
not counted. Source files and commands were neither changed nor executed.
The [aggregate census](acceptance/menu-census-2026-10-03.json) excludes private
paths, menu text and actions; all 42 source hashes were rechecked unchanged.

This immediately adds an invalid-size case, permission-sensitive menus and
multiline hover content to the corpus. Preserve source hashes; use isolated
copies. Never execute a shop/payment/permission action from an original menu
merely to obtain a screenshot. Behavioral fixtures must use controlled test
state while preserving the layout and conditions being compared.

## Implementation order

1. Build a common text/layout representation that retains font identity, exact
   advances, fractional coordinates, tint and ordered image/text layers.
   Resolve actual inventory sizes from source/runtime information; do not guess
   solely from decorative grids. Keep namespace-qualified image identities.
2. Use that representation in a Bedrock inventory adapter. Reproduce the current
   wide-title failure first, then 1-6 rows, transparent holes, multiple layers,
   tooltips and slot interactions. Validate desktop and touch independently.
3. Connect provider HUD events and variables to the same layout rules. Verify
   animation timing and data changes; compare recordings rather than one frame.
4. Expand equipment, block and model behavior alongside the existing item/display
   regressions. Separate source-format support from provider runtime support.
5. Add topology/device/resource gates before broad release claims. A client
   limitation requires a measured reproduction and a stated emulation boundary;
   an external vendor's limitation is not proof of impossibility for Twilight.

Every product change must bring a reproducible regression, current source/build
hashes and affected client checks. A full test rerun alone cannot turn a planned
feature into support. The existing snapshot JAR and screenshots are unchanged
by this research/catalog commit; no new gameplay conversion feature is claimed.
