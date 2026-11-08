# تطبيق ورشة فوري للخدمات الميدانية والصيانة (`warsha-now-services-1290126`)
> منظومة services المتكاملة مع ربط Mada Contactless EMV L2 NFC + GPS Dispatch

- **Project ID**: `#1290126`
- **Commercial Archetype**: `services` (On-demand technicians, mobile workshops, roadside repairs, HVAC dispatch)
- **Bound Hardware Peripheral**: `Mada Contactless EMV L2 NFC + GPS Dispatch`
- **Package Name**: `sa.factory.warshanowservices1`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update]

### 1. Architectural & Production Engines
- **`MadaEmvContactlessProtocolEngine`**:
  * Saudi Payments (Mada) contactless specification engine and ISO 7816 / EMV Level 2 kernel.
  * BER-TLV (Tag-Length-Value) hexadecimal parser (`TAG_PAN` 0x5A, `TAG_AMOUNT_AUTH` 0x9F02, `TAG_CRYPTOGRAM` 0x9F26).
  * Atheer contactless CVM logic (PIN bypassed for transactions under 300 SAR; online PIN mandatory above threshold).
  * Cryptogram generation (ARQC / Transaction Certificate TC) and dynamic authorization code formatting.
  * PCI-DSS cardholder PAN masking.

- **`GpsMobileDispatchEngine`**:
  * Geodesic navigation engine calculating exact distance using the Haversine formula ($R = 6371$ km).
  * Dynamic urban ETA estimation with Saudi municipal traffic congestion factors.
  * 5-stage mobile workshop service state machine (`REQUESTED`, `ASSIGNED`, `EN_ROUTE`, `ON_SITE`, `COMPLETED`).
  * Live mobile workshop fleet tracking (technician ID, van license plate, distance km, transit ETA minutes).

### 2. UI & Driver Modernization
- **`HardwareSensorDriver`**: Dual-engine telemetry loop streaming real-time Mada EMV L2 transaction approvals, Atheer CVM status, van license plate, geodesic GPS distance, and dynamic ETA into ViewModel StateFlows.
- **`MainActivity` & Compose UI**: Fixed missing typography imports, resolved single-quote template hazards, migrated deprecated `Divider` to `HorizontalDivider`, and eliminated non-standard unicode glyphs (checkmarks replaced with `[OK]`, crosses with `[X]`, bullets with `|`).
- **`WarshaNowViewModel`**: Enhanced `checkout(paymentMethod: String)` signature for dynamic Mada / Apple Pay POS processing.
- **`AndroidManifest.xml`**: Modernized application label to `ورشة فوري للصيانة الميدانية - Warsha Now`.

### 3. Unit Regression Test Suite
- Comprehensive 10-case JUnit test suite in `sa.factory.warshanowservices1.WarshaNowRegressionTest`:
  * `test01_berTlvHexParser`
  * `test02_madaAtheerUnder300SarNoCvm`
  * `test03_madaAbove300SarRequiresOnlinePin`
  * `test04_madaCardMaskingAndCryptogramGeneration`
  * `test05_haversineDistanceCalculationAccuracy`
  * `test06_urbanEtaCalculationWithCongestionMultiplier`
  * `test07_dispatchMissionCreationAndStateTransition`
  * `test08_geodesicZeroDistanceSelfCheck`
  * `test09_hardwareSensorDriverDualEngineStream`
  * `test10_repositoryCartAndVatCalculation`

### 4. Build and Test Instructions
```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Regression Tests
./gradlew testDebugUnitTest
```
