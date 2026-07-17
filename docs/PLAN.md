# Tech Arsenal v0.1 実装プラン

Forge 1.20.1 / mod_id `techarsenal` / パッケージ `com.girigiri.techarsenal`

## 設計方針

- **ネットワークパケットなし**で成立する構成にする(v0.1)。カメラ視点はクライアント側だけで完結させ、
  エンティティ(ミサイル/ドローン)はバニラの同期機構に乗せる。
- テクスチャは16x16のピクセルアートをスクリプト生成(`tools/generate_textures.ps1`)。
- 監視カメラの閲覧は**64ブロック以内**に制限(遠隔チャンクのロードは行わないため)。

## 機能仕様

### 1. 監視カメラ (Security Camera)
- `SecurityCameraBlock` — 水平4方向を向く小型ブロック。BlockEntityなし。
- `CameraMonitorItem` — カメラブロックを右クリックでリンク(座標+向きをNBT保存)。
  空中で右クリックするとリンク先のカメラ視点に切替(64ブロック以内のみ)。Shiftで解除。

### 2. 衛星カメラ (Satellite Camera)
- `SatelliteRemoteItem` — 使用すると自分の真上80ブロックから真下を見下ろす衛星視点に切替。Shiftで解除。

### 3. 誘導ミサイル (Guided Missile)
- `MissileLauncherItem` — 使用時、視線方向のモンスター(48ブロック以内・視線コーン内)をロックオンして
  `GuidedMissileEntity` を発射。インベントリの `guided_missile` を1個消費(クリエイティブは消費なし)。クールダウン30tick。
- `GuidedMissileEntity` — 毎tickターゲットへ誘導。着弾で爆発(半径2.0、**ブロック破壊なし** ExplosionInteraction.NONE)。寿命200tick。

### 4. 戦闘ドローン (Combat Drone)
- `DroneItem` — 地面に使用でドローン召喚(使用者がオーナー)。
- `DroneEntity` — 飛行Mob(FlyingMoveControl+noGravity)。オーナー追従(10ブロック超で接近、32ブロック超でテレポート)。
  モンスターを自動索敵し `DroneBoltEntity`(小型弾)で射撃。オーナーがスニーク右クリックでアイテムに回収。

### カメラ視点の仕組み(共通)
- `CameraEntity` はクライアント側でのみ生成し、ワールドには追加せず `Minecraft#setCameraEntity` に渡す。
- 視点中は `MovementInputUpdateEvent` で移動入力をゼロ化し、Shift押下で元の視点に復帰(`ClientCameraHooks`)。

## ファイル構成

```
com.girigiri.techarsenal
├ TechArsenal.java            … 各レジストリの登録のみ
├ registry/ ModBlocks, ModItems, ModEntities, ModCreativeTabs
├ block/    SecurityCameraBlock
├ item/     CameraMonitorItem, SatelliteRemoteItem, MissileLauncherItem, DroneItem
├ entity/   CameraEntity, GuidedMissileEntity, DroneEntity, DroneBoltEntity
└ client/   ClientSetup, ClientCameraHooks, renderer/(DroneRenderer, NoopRenderer)
```

アセット: blockstates + block/itemモデルJSON、テクスチャ7枚、lang(en_us/ja_jp)、
data(レシピ6種、security_cameraルートテーブル、mineable/pickaxeタグ)。

レンダラー: ミサイル/弾は `ThrownItemRenderer`(アイテムスプライト描画)を再利用。
ドローンはアイテムを浮遊描画する軽量カスタムレンダラー。カメラは非描画。

## 検証

1. `gradlew build` 成功 + `build/libs/techarsenal-1.0.0.jar` 生成
2. コミットして GitHub へプッシュ

## v0.2: モニターブロック(実装済み)

- **MonitorBlock + MonitorBlockEntity** — 同じ向きの隣接モニターを最大5x5の矩形に連結
  (`MonitorScreen.resolve` が下端・視聴者左端のブロックをコントローラーに選出)。
  右クリックでフィード切替: OFF → SAT → CAM-1 → CAM-2 …(スニークで逆順)。
- **CameraRegistry (SavedData)** — 監視カメラ設置時に CAM-n を自動採番。ディメンション毎に永続化。
- **ライブ映像** — クライアント側 `FeedManager` が RenderTickEvent(START) でカメラ視点から
  `GameRenderer#renderLevel` をオフスクリーン RenderTarget に再描画(0.5秒間隔・1フレーム1キャプチャ)。
  `MonitorBlockEntityRenderer` が連結画面全体にテクスチャを貼り、左上にIDラベルを描画。
  Fabulous!グラフィックスでは transparency chain と干渉するためキャプチャ停止(最終フレーム保持)。
- **ID表示** — モニター画面ラベル(SAT / CAM-n / NO SIGNAL / OFF)、携帯モニターのリンクメッセージと
  カメラ視点のアクションバーにも CAM-n を表示。

## v0.3: 武器・兵器・ビークル(実装済み)

- 携行武器7種(ライフル/マシンガン/グレネードランチャー/ロケットランチャー/レーザーガン/ビームサーベル/火炎放射器)。
  スペックは docs/WEAPONS.md を正とする。マシンガン・火炎放射器は bow 方式のホールド射撃(`onUseTick`)。
  レーザーは hitscan(`level.clip` + `ProjectileUtil.getEntityHitResult`)。
- 地雷(`entityInside` で起爆)、防衛タレット(RangedAttackGoal 速度0)、
  アパッチ(travel オーバーライドで視線方向フライト)、戦車(tickRidden/getRiddenInput 方式)。
  注意: LivingEntity#travelRidden は 1.20.1 では private のためオーバーライド不可 — travel() で分岐する。
- ビークル/タレットは EntityModel(コード定義のキューブモデル)+ MobRenderer で描画。
  ヘリのローターは setupAnim で回転。エンティティテクスチャはスクリプト生成のカモ柄フィル。
- 機械系は溺れ無効(canBreatheUnderwater)+ 炎耐性(fireImmune)。

## v0.4: ビームサーベル強化・搭載武器・弾薬・支援装備(実装済み)

### ビームサーベル
- **ビーム描画** — `builtin/entity` モデル + BEWLR(`BeamSaberRenderer`)。グリップは通常ライティングの
  キューブ、ブレードは白コア+シアンの発光グロー(`RenderType.entityTranslucentEmissive` +
  FULL_BRIGHT)。グローはゲーム時間で脈動。
- **弾き(パリィ)** — サーバー側 `SaberDeflection`(PlayerTickEvent)。スイング中
  (`player.swinging`)にサーベルを持っていると、正面〜側面(視線との内積 > -0.35)から
  接近中の飛翔体(半径2.5m)を弾く。弾いた弾は速度反転(射手がいれば射手へ向け直す)+
  所有者を弾いたプレイヤーに変更(反射ダメージが自分の攻撃扱いになる)。
- **弾かれエフェクト** — 発光アウトライン(setGlowingTag)+ELECTRIC_SPARK/CRITバースト+
  シールド音/アメジスト音。弾かれた自作弾(弾丸/ボルト)は以降 END_ROD の軌跡を引く。
- **レーザー(hitscan)の弾き** — `LaserGunItem` 側で被弾者がパリィ中なら無効化し、
  ビームを射手に反射(射手が7ダメージを受ける)。

### ビークル搭載武器(ネットワーク基盤)
- `ModNetwork`(SimpleChannel)を新設。搭乗中に **Rキー**(`key.techarsenal.vehicle_fire`)で
  `FireVehicleWeaponPacket` を送信 → サーバーで `ArmedVehicle#fireWeapon`。長押しで連続射撃。
- ヘリ: ロケットポッド(左右交互、ロケット弾、CD 20t)。戦車: 主砲(`ShellEntity` 直撃15+爆風3.0m、CD 60t)。
- 搭乗武器は弾薬不要。搭乗時にアクションバーへ操作ヒント表示(`Component.keybind` でキー名解決)。

### 弾薬システム
- 既存の表示用アイテムを弾薬化: `bullet`(ライフル/マシンガン)、`grenade`、`rocket`。
  サバイバルではインベントリから消費、クリエイティブは無消費(`AmmoHelper`)。
  マシンガンは弾切れで自動停止。レーザーガン/火炎放射器はエネルギー式のまま(弾薬なし)。
- クラフトレシピ: 弾丸x8(鉄+火薬)、グレネード弾x2、ロケット弾x2。

### レーザーデジグネーター
- 右クリックで視線先(64m)の生物をマーキング(NBTにUUID+時刻、有効60秒、対象は発光60秒)。
  スニーク右クリックで解除。ミサイルランチャーはデジグネーター指定目標を最優先で追尾
  (視線ロック不要・128mまで)。

### ドローンアップグレード
- モジュールを持ってドローンを右クリックで適用(各最大Lv2、モジュール消費):
  攻撃モジュール=ボルト威力 5→8→11、装甲モジュール=HP 20→30→40(適用時全回復)。
- レベルはエンティティNBTに保存、スニーク回収したアイテムにも引き継ぎ(ツールチップ表示)。

### モニター
- フィードのアスペクト比補正: 画面比とテクスチャ比が異なる場合はセンタークロップ(UV調整)。

## v0.5: セキュリティシステム・弾薬拡張(実装済み)

### 顔認証自動ドア
- **顔認証スキャナー (`face_scanner`)** — カメラ型の設置ブロック(BlockEntity、5tick毎に判定)。
  正面3ブロックの検知ゾーンに立つプレイヤーを「顔スキャン」し、登録済みなら認証OK。
  - 設置者=オーナーで自動登録。オーナーが素手右クリックで**登録モード**(30秒):
    その間にゾーンへ立った人を顔登録。スニーク右クリックで全登録を消去。
  - **認証OK**: 緑のパーティクル+チャイム音+アクションバー表示、半径4ブロック内の
    ドア(セキュリティドア/鉄のドア等)を自動で開き、ゾーンから離れて1.5秒後に自動で閉める。
  - **認証NG**: 赤のパーティクル+ブザー音+「認証NG」表示(2秒クールダウン)。ドアは開かない。
- **認証モニター (`auth_monitor`)** — スキャナーの半径4ブロック内に置くと連動する表示板。
  ブロックステートで画面が切替: 待機(青の顔アイコン)/ 認証OK(緑のチェック)/ 認証NG(赤のX)。

### 鍵付き扉
- **セキュリティドア (`security_door`)** — 鉄製ドア(レッドストーンでは開かない設計)。
  未施錠なら誰でも開閉可。**鍵 (`door_key`) で右クリックすると施錠**(鍵に固有IDを刻印し
  LockRegistryに登録)。施錠後は同じIDの鍵でのみ開閉、鍵なしはブザー+「施錠されています」。
  施錠した鍵でスニーク右クリックすると解錠。破壊すると施錠情報も消える。
- **LockRegistry (SavedData)** — ドア座標→鍵UUIDのマップ(`techarsenal_locks`)。
- 顔認証スキャナーは施錠状態に関係なくセキュリティドアを開けられる(顔認証自動ドア)。

### 弾薬・燃料の拡張
- ヘリのロケットポッド=ロケット弾、戦車の主砲=戦車砲弾(レシピ追加)を搭乗者の
  インベントリから消費。火炎放射器=ブレイズパウダー(使用開始時+2秒毎に1個)。
  いずれもクリエイティブは無消費。

## v0.6: ビジュアル&サウンド大改修(実装済み)

- **ビークルモデル改修** — 戦車: 転輪x5/サイドスカート/段付き車体(グレイシス+エンジンデッキ)/
  砲塔+キューポラ+防盾/マズルブレーキ/同軸機銃/アンテナ。アパッチ: 段付き機首+チンガン/
  ガラスキャノピー/エンジンハウジング/スタブウィング+ロケットポッド/テールローター(回転)/
  スキッド+支柱/4枚ブレードのメインローター+ハブ。テクスチャは迷彩ブロブ+履帯トレッド+ガラス領域。
- **銃器6種の3Dモデル化** — `tools/generate_gun_models.ps1` がJSON elementsモデルを生成
  (パレットテクスチャ `gun_palette.png` のスウォッチをUV参照)。銃身は-Z向きで構築。
- **発射エフェクト** — ライフル/マシンガン発射時にマズルフラッシュ(FLAME)+硝煙(SMOKE)+
  **薬莢排出**(金塊のItemParticleで右方向へ射出)。戦車主砲はフラッシュ増量。
- **カスタム効果音** — `tools/generate_sounds.ps1` がffmpegでOGG合成:
  machine_gun_fire(ダダダ連射音)/rifle_fire/tank_cannon(重低音)/saber_swing(フォン)/
  saber_special(ヴゥォン)。ModSounds+sounds.jsonで登録。
- **ビームサーベル特殊技(Rキー)** — SaberSpecialPacket: 周囲3mに12ダメージ+ノックバックの
  360°回転斬り。SWEEP_ATTACK+END_RODのリングエフェクト+「ヴゥォン」音+クールダウン5秒。
  スイングのたびに「フォン」音(クライアント側 swinging エッジ検出)。

## v0.7: プレイヤーマニュアル(実装済み)

- **プレイヤーガイド文書** — `docs/MANUAL_JA.md` に全機能の日本語プレイヤーズマニュアルを追加。
  数値の正は引き続き `docs/WEAPONS.md`。
- **取扱説明書アイテム (`field_manual`)** — 使用するとバニラの `BookViewScreen` を開き、
  ガイドの要約(条件付き改行込みの `techarsenal.guide.*` lang キー、セクション毎に
  複数ページ)を読める。`client/gui/GuideBookContent`(ページ構成)+
  `client/gui/GuideBookOpener`(`BookAccess` の匿名実装で画面を開く)。アイテム側は
  `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` 経由でのみクライアントクラスに触れる
  (専用サーバーでのクラスロードクラッシュを回避)。クリエイティブタブの先頭に配置。
- **注意(数値の二重管理)** — 武器・設置兵器・ビークルの数値は `docs/WEAPONS.md`(正)・
  `docs/MANUAL_JA.md`・`techarsenal.guide.*` の lang 文字列の3箇所に登場する。
  今後スペックを変更する際はこの3箇所を同時に更新すること。

## v0.7.1: シェーダー使用時の空チラつき修正(実装済み)

- **不具合** — OptiFine/Iris系のシェーダーパック(Sildur's Vibrant Shaders 等)導入時、
  モニターにフィードを表示していると画面全体の空が毎フレームチラつく(雨天時に悪化)。
  原因は `FeedManager#onRenderTick` がモニター用オフスクリーンキャプチャのために毎フレーム
  `GameRenderer#renderLevel` を**2回目**呼んでおり、シェーダーパックがフレームまたぎで
  保持しているグローバル状態(フレームパリティ・時間積分バッファ・天候ユニフォーム等)を
  汚染していたため。安全なキャプチャ間隔は存在しない(間引き=一定周期のチラつきに
  なるだけ、コミット d30a27a で確認済み)ため、シェーダー使用中はキャプチャを**全面停止**する
  方式にした。
- **修正** — `FeedManager.onRenderTick` に `shadersActive()`(OptiFine/Iris検出、既存)による
  早期returnを追加(Fabulous!グラフィックス用の早期returnの直後)。`evictStale` は
  シェーダー使用中も引き続き実行(オフスクリーンGPUリソースは解放され続ける)。
  シェーダー使用中はフィードが最終キャプチャフレームで静止(未キャプチャなら NO SIGNAL)し、
  シェーダーをOFFにすると次フレームから自動的にライブ復帰する。非シェーダー時の挙動は変更なし。
  モニターのラベルはシェーダー使用中 `[SHADERS]` を付与して静止中であることを明示。

## v0.7.2: シェーダー使用時もモニターをライブ更新(マスクドキャプチャ方式)(実装済み)

- **方針転換** — v0.7.1 の「シェーダー使用中はキャプチャ全面停止(静止表示)」を、
  **チラつきゼロを保ったままライブ映像に戻す**方式に置き換えた。根拠はコミット d30a27a の実測:
  シェーダー使用中に `renderLevel` を余分に1回呼んでも、汚染されるのは**その直後に表示される
  1フレームだけ**で、パイプラインは次フレームで自己回復する。連続チラつきは毎フレーム
  キャプチャしていたときにだけ発生していた。したがって「汚染される1フレームをユーザーに
  見せない」ようにできれば、キャプチャはシェーダーパイプラインを通して実写のまま走らせられる。
- **仕組み(マスクドキャプチャ)** — シェーダー使用中は約 `SHADER_REFRESH_INTERVAL_FRAMES`(=100、
  60fpsで約1.7秒)ごとに1回だけ実キャプチャを許可する。キャプチャの**1フレーム前**(`RenderTickEvent`
  Phase.END)で、合成済みのメインフレームバッファをフル解像度のバックアップ `frameBackup` に退避。
  キャプチャフレーム本体では既存の `capture()` を Phase.START でそのまま走らせる(このフレームの
  シェーダー状態だけが汚染される)。そして画面表示(`blitToScreen`)の**直前**の Phase.END で
  バックアップをメインターゲットへ blit し戻し、汚染フレームの代わりに前フレームを再表示する。
  結果、モニター表示中およそ1.7秒に1回「1フレームだけ前フレームが重複表示される」だけで、
  これはフレームドロップと区別がつかず知覚できない。フレームカウンタのオフバイワン
  (バックアップ/マスク/実キャプチャの各フレーム番号の一致)に注意して実装。
- **フックの選定** — Forge `Minecraft.runTick` を確認し、`RenderTickEvent` Phase.END は
  `gameRenderer.render`(ワールド+HUD描画)完了後・`mainRenderTarget.blitToScreen`/
  `updateDisplay`(画面提示)前に発火することを確認済み。よってバックアップ退避・復元 blit の
  両方を Phase.END(と Phase.START のキャプチャ)で完結でき、`RenderGuiEvent.Post` 等の別フックは不要。
- **OptiFine逆コンパイル確認** — `OptiFine_1.20.1_HD_U_I6.jar` を展開し `net/optifine/**` を確認。
  `net.optifine.Config.isShaders()`(`public static boolean`)が存在し、`Shaders` 側のシェーダー
  ロード状態を参照していることを確認(既存 `shadersActive()` の妥当性を裏付け)。`Shaders.frameCounter`
  (`static int`)/`frameTimeCounter`(`static float`)も存在するが**package-private**であり、
  任意のオプション追加(カウンタのスナップショット/復元)は**採用しなかった**:正当性はマスキング
  自体が担保しており、バージョン差の大きい OptiFine 内部フィールドへの追加リフレクション結合は
  脆さを増すだけと判断したため。OptiFine のシェーダーONフラグを一時反転させる案も設計方針どおり
  不採用(シェーダーON前提でビルドされたterrain VBOの頂点フォーマットに不整合を起こしうるため)。
- **フォールバック(二重の非常停止)** — コンパイル時定数 `SHADER_LIVE_FEED` を `false` にして
  再ビルドすると、他のコード変更なしで **78493d4 相当の静止表示挙動に完全に戻る**(制御フローが
  厳密に縮退することを確認済み)。実行時フラグ `shaderLiveBroken` は、マスキング経路で例外が
  発生した場合に一度だけログ出力してセットされ、以降そのセッションは静止表示へ安全に自動退行する
  (クラッシュ・毎フレームのログ連投なし)。ラベルはライブ動作中 `[SLOW]`、静止時 `[SHADERS]` で区別。
- **検証状況(正直な記載)** — **非シェーダー経路のみ dev(runClient)で確認済み**:クライアントが
  タイトル画面まで正常起動し、MOD ロード・イベント登録に異常なし、`gradlew build` 成功。
  非シェーダー経路とFabulous!経路のコードは実質不変(`SHADER_LIVE_FEED` 有効時も
  `shadersActive()==false` なら従来と同一分岐)。dev環境への OptiFine 製品jar投入は従来同様
  `IOException: Base resource not found: <難読名>.class` で機能ロードせず(Mojangマップのdev
  ランタイムと難読クラス名の不整合)、シェーダー中のマスク動作は dev では検証不能。
  **シェーダー使用中の実挙動(空チラつきゼロ・ライブ更新)は実プレイ環境での確認が必要。**

## v0.9.2: Vキー視点の自動解除バグ修正 + 視点パン + ズーム(実装済み・dev検証済み)

計画=Fable5、実装=別モデル、検証=オーケストレーターの標準ワークフロー。コミット(次)。

### バグ: Vキー視点が狭所で約3秒後に自動解除される
**真因(Fable5がデコンパイル済みForgeソースで確定)**: Vキー視点は`ServerPlayer#setCamera`で
本体をカメラ位置(`viewPosFor`=ブロック中心+facing×0.6)へテレポートするが、クライアントの
`LocalPlayer`は重力で落下し続けmove packetを送る(`onMovementInput`は移動キーだけ0にし重力は
止めない)。サーバーの`ServerPlayer.tick()`は毎tick`absMoveTo`で本体をカメラへ引き戻す。**狭所
(視点位置の1-2ブロック下に床)では**クライアントが毎サイクル着地して`onGround=true`を送り、
"moved wrongly"補正はfallDistanceをリセットしないため幻の落下距離が蓄積、約3サイクル(~60tick=3秒)で
3.0を超え落下ダメージ→`LivingAttackEvent`→`onLivingAttack`が全ダメージで`close()`していた。開けた
場所では着地しないので落下ダメージゼロ=解除されない(症状の場所依存性が一致)。副次的に、天井の
低いカメラでは窒息(`inWall`)、水中では溺れでも同様に即/漸次解除。**修正**: (1)`onLivingAttack`を
「攻撃者エンティティ有り(`source.getEntity()!=null`)または`BYPASSES_INVULNERABILITY`(/kill・奈落)の
時だけ`close()`、それ以外の環境/自傷ダメージはキャンセルして視点維持」に変更(v0.9の「攻撃で一撃
キャンセル+視点終了」の設計意図は本物の攻撃に対して維持)。(2)`onPlayerTick`で毎tick
`player.resetFallDistance()`+`setAirSupply(max)`し落下距離/空気が育たないようにする。(3)`close()`で
復帰テレポート後にも`resetFallDistance()`。**dev実測確認**: `/damage Dev n minecraft:fall|drown|in_wall`
では視点維持、`/damage ... by @e[armor_stand]`(攻撃者付き)では視点解除。**トレードオフ**: 視聴中は
本体が環境ダメージ(溶岩・炎など)に無敵になる(Mob/プレイヤーの攻撃では解除される)。

### 視点パン(マウスで見回し)
`ViewportEvent.ComputeCameraAngles`で描画カメラ角を`LocalPlayer`のマウス連動yaw/pitchに差し替える
(バニラ`MouseHandler.turnPlayer`はスペクテイト中もLocalPlayerを回すので角度は既に蓄積している)。
`onClientTick`で角度を**パンコーン(yaw±100°/pitch-45〜+80°、ブロックfacing基準)にクランプ**し、2tick毎・
1°以上変化時に新パケット`PanCameraViewPacket`(C2S)でサーバーの`CameraEntity`回転も同期
(サーバー真実を維持。`RemoteCameraView.pan`が同じ範囲で再クランプ)。カメラ切替(左右クリック)時は
新カメラのfacingへスナップ。**壁掛けフィード非干渉**: 両ビューポートイベントで`FeedManager.isCapturing()`を
ゲート(フィードキャプチャはカメラエンティティを差し替えて同イベントを再発火するため)。SATビューは
`ce==camera`で除外し従来通り固定。終了時は`close()`の復帰テレポートで元の視線に戻る。

### ズーム
`ViewportEvent.ComputeFov`でFOVを`/ZOOM_LEVELS[zoomIndex]`(段階{1,2,4,8})。`InputEvent.MouseScrollingEvent`を
`remoteActive`中は常に`setCanceled(true)`してホットバー切替を奪い、上=ズームイン/下=アウトで1イベント1段。
`usedConfiguredFov()`と`isCapturing()`でゲート。ヒットバー保護・アスペクト非破壊。ヒントに`CAM-n x4`のように
倍率を表示(新langキー不要)。視点開始/終了/カメラ切替でzoomIndexを0にリセット。**既知の制限**: 高倍率でも
マウス感度は下がらない(バニラのスコープ感度低下は`isScoping()`専用)。**dev実測確認**: パン=マウスで視点回転、
ズーム=スクロールでFOV縮小(×4表示)、スクロールでホットバー不変、スニークで元位置へ復帰。**シェーダー環境の
最終確認待ち**(OptiFine/Iris下でのパン・ズーム描画干渉はdev再現不可)。

## v0.9.1: シェーダー時のモニター手写り込み・カメラ視点暗転の修正(実装済み)

ユーザーが実プレイ(OptiFine+シェーダー)で報告。2段階で解決した。第1段(コミットf9fdb46)で
カメラ視点の暗転は解消したが、本体を隠したことで奥に隠れていた「プレイヤーの手」が両視点に露出。
第2段(本節)で手の描画源を突き止めて除去。バグの正体は「プレイヤーモデル/一人称手がカメラ映像に
写り込む」ことで、シェーダー固有ではなく描画イベントで確実に対処できた。

- **カメラ視点(Vキー)の暗転** — ユーザーの指摘「カメラがアバターの中にいるのでは？」が的中。
  v0.9のスペクテイトは視聴中プレイヤー本体をカメラ位置へテレポートするため、視点(カメラエンティ
  ティ)が自分のプレイヤーモデルの脚の内側に埋まり、バニラは「カメラ≠プレイヤー」時に自分のモデルを
  描画するのでモデルの暗い内側がレンズを覆っていた(明るさ算出の問題ではなかった)。**修正(第1段)**:
  `ClientCameraHooks.onRenderPlayer`(`RenderPlayerEvent.Pre`)で`remoteActive`中は自分の描画を
  `setCanceled(true)`。
- **本体を隠した後も残る「手」(両視点共通)** — 本体を消したら、その奥にあった描画が露出した。
  正体は2種類:
  - **Vキー視点の手 = 一人称の手**(`renderItemInHand`)。本体モデルとは別レンダリングなので
    `RenderPlayerEvent`では消えない。**修正**: `ClientCameraHooks.onRenderHand`(`RenderHandEvent`)で
    `active || remoteActive`中は`setCanceled(true)`。dev検証: CAM視点から持ち物(camera_monitor)の
    一人称手が完全に消えることを確認(修正前は右下に写っていた)。
  - **壁掛けモニターのフィードには2つが写っていた**: (a)三人称プレイヤー本体の腕/持ち物
    (`onRenderLevelStage`の「自分を映す」描画、カメラが近いと大写し)と (b)シェーダー時に
    `blitMainIntoFeed`がメインバッファをコピーする際に混入する一人称の手。**(a)の修正**:
    `onRenderPlayer`の条件を`remoteActive || FeedManager.isCapturing()`に拡張し、フィード
    キャプチャ中も自機本体描画をキャンセル(ユーザー確認: 本体は消えた)。**(b)の修正**:
    `capture()`のシェーダー分岐で`renderLevel`前に`mc.getMainRenderTarget().clear()`し、前フレームの
    一人称手が残るメインをクリアしてからカメラ視点だけをコピー。加えて保険として`onRenderHand`の
    条件に`|| FeedManager.isCapturing()`を追加(OptiFineがオフスクリーン`renderLevel`内で手パスを
    描く場合に備える)。**(b)は要シェーダー環境の最終確認**。もし手が残る場合の最終手段は
    シェーダー時のフィード凍結(`SHADER_LIVE_FEED=false`=v0.7.1相当、ライブ更新は失うが手は確実に
    消える)。他エンティティ(他プレイヤー/Mob)は通常通り映る。
- **SAT視点は不変**(`active`のみ、`isCapturing`/`remoteActive`ではない)。俯瞰で自分を確認できる
  よう本体は描画を維持(一人称手だけは`RenderHandEvent`で抑制)。
- **すべてForgeの描画イベントベースなのでシェーダー有無を問わず効く**。dev(非シェーダー)で
  一人称手の除去・本体除去・解除後の正常復帰を確認済み。壁掛けフィードの手除去はシェーダー環境で
  最終確認が必要(修正機構は第1段の本体隠しで実証済みの`RenderPlayerEvent.Pre`の条件拡張)。

### 追補: 壁掛けフィードから三人称本体が消える回帰の修正

上記(a)の修正(`onRenderPlayer`の条件を`remoteActive || FeedManager.isCapturing()`へ拡張)が
副作用を起こしていた。`FeedManager.isCapturing()`中に`RenderPlayerEvent.Pre`を
`setCanceled(true)`すると、Forgeは`LevelRenderer`にパッチを当てて「視点エンティティが
プレイヤー自身でなくてもローカルプレイヤーを描画する」ようにしているため、このキャンセルは
壁掛けモニターが本来映すべき三人称本体そのものを消してしまっていた(「自分が監視カメラに
映る」機能の根幹)。**修正**: `onRenderPlayer`の条件を`remoteActive`のみに戻し、
`isCapturing()`中はキャンセルしない。一人称の手は引き続き`onRenderHand`
(`active || remoteActive || FeedManager.isCapturing()`)と`FeedManager.capture()`の
シェーダー分岐にある`mc.getMainRenderTarget().clear(Minecraft.ON_OSX)`(メインバッファを
クリアしてから視点だけをコピー)で消えたままなので、手写り込みの修正自体は失われていない。
ライブシェーダーフィード(`SHADER_LIVE_FEED`)の仕組みも無変更。**既知のトレードオフ**:
カメラのすぐそばに立つとフィード内で自分の三人称の腕が大写しになるが、これは「監視カメラに
自分が映る」という仕様どおりの挙動であり不具合ではない。将来これが気になるようなら、
`onRenderPlayer`で`isCapturing()`をまたキャンセル対象に戻すのではなく、`onRenderLevelStage`
側にカメラからの距離が概ね1.5m未満なら本体描画をスキップする、という近接判定を足す方向で
対処すること。

### 追補2: 壁掛けフィードに本体が描かれない真因の修正(コミットb6a4229)

上記f8d0988でも**非シェーダー環境では本体がフィードに一切描かれない**ことが、フィードFBOの
直接ダンプで判明。**真因(FBOバインド診断で確定)**: `FeedManager`のオフスクリーン
`renderLevel`中、バニラは`AFTER_ENTITIES`ステージまでに**メインのフレームバッファへ再バインド**
する(`boundFbo`=メイン、`feedFbo`=フィードで不一致を実測)。そのため`onRenderLevelStage`の
自機描画もバニラ/Forgeのエンティティ描画も、**自機を含む全エンティティがメインターゲットに
落ち、`feed.target`には届かない**。地形/ブロックはそれより前(feed.targetがまだバインド中)に
描かれるので写る。シェーダー有効時は`blitMainIntoFeed`がメイン→フィードをコピーするので本体が
写っていた=**ユーザーのOptiFine環境でだけ見え、devでは原理的に一度も見えなかった**。よって
f8d0988の`RenderPlayerEvent.Pre`キャンセル撤回だけでは不十分だった。**修正**: `FeedManager`に
`public static RenderTarget captureTarget`を公開し、`onRenderLevelStage`は自機描画の直前に
`captureTarget.bindWrite(true)`でフィードターゲットを再バインドしてから描画、直後に退避した
FBO(`GL_FRAMEBUFFER_BINDING`)とビューポートを正確に復元して`renderLevel`の後続パスを乱さない。
シェーダー時は再バインドしても続く`blitMainIntoFeed`で上書きされるが本体はメインにも居るため
写る=冗長だが無害。**dev実測確認済み**: CAM-15フィードに三人称本体が描画・移動追従(ライブ)・
メイン画面無影響。**シェーダー環境の最終確認待ち**: (1)壁掛けに本体が映る(2)一人称の手は映らない
(3)フィードがライブ更新`[SLOW]`。**教訓**: モニターフィード(オフスクリーンrenderLevel)に
任意のものを描き込むときは、`AFTER_ENTITIES`以降ではメインFBOがバインド済みなので、フィード
FBOへ明示的に再バインドしてから描く必要がある。フィード内容の検証はモニター面のスクショ切り
出しでは不正確で、`NativeImage`での`feed.target`直接PNGダンプが確実(検証後は削除)。

## v0.8: カメラモニターの複数登録+クリック切替(実装済み)

- **複数カメラ登録** — `CameraMonitorItem` が1台しか覚えられなかった制約を撤廃。NBTを
  ルート直下のスカラー(`CameraPos`/`CameraYaw`/`CameraId`)から `ListTag "Cameras"`
  (各エントリは同名タグを流用したコンパウンド)+ 選択中IDを持つ `SelectedCameraId` に変更。
  カメラを右クリックで追加(`BlockPos` で重複排除、上限 `MAX_CAMERAS`=24、`getOrAssign` で
  既存の `CameraRegistry` からID取得)、スニーク右クリックで解除(選択中IDが消えたら残りの
  最小IDへ自動フォールバック、全解除でタグごと削除)。この追加/解除ロジックは
  `CameraMonitorItem.useOn(UseOnContext)` の中に実装しており、`DoorKeyItem.useOn` の
  既存実装で確認済みの通り、バニラはスニーク時にブロック側 `use()` はスキップしてもアイテムの
  `useOn`/`UseOnContext` 経路はスキップしない(dev runClientで実際にスニーク右クリックが
  `useOn` に到達し `camera_unlinked_one` メッセージが出ることを確認済み — 詳細は下記検証状況)。
- **旧アイテムの移行** — `readCameras()` はクライアントでも呼べる非破壊の読み取りで、
  新形式が無く旧ルートスカラーだけがある場合はメモリ上で1件のリストに変換して返す(NBFは
  書き換えない)。実際の書き換え(`migrateLegacyTag`)はサーバー側の書き込み経路
  (アイテムの `useOn` と新パケット `SelectCameraPacket` の `handle`)の先頭で必ず呼び、
  旧タグを新形式に一度だけ変換して削除する。
- **視聴中のマウスクリック切替** — Vキーで視点を開いている間、左クリックでID降順・
  右クリックでID昇順に隣のカメラへ切替(ラップアラウンド)。ブロックが破壊済み、または
  プレイヤーの**現在位置**から `MAX_VIEW_DISTANCE`(64m)を超えているエントリは切替候補から
  スキップ(保存された有効性を信用せずその場で再チェック)。探索は `list.size()` 回で
  打ち切るためハングしない。候補が無ければ `camera_no_switch_target` を表示し現在の視点を維持。
  実体の `CameraEntity` は使い回し(`activate()`/`deactivate()` を再呼びせず `moveTo` +
  `xo`/`yo`/`zo`/`yRotO`/`xRotO` の更新のみ)。
- **クリック横取り** — `net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered`
  (`@Cancelable`・クライアント専用・Forgeイベントバス)を購読し、`isActive()` 中のみ
  `setCanceled(true)` + `setSwingHand(false)` で全クリックを無効化(SAT視点中も同様に無効化し、
  ブロック破壊/設置が漏れる既存の不具合も同時に修正)。`ClientCameraHooks` に
  `activeHand`(CameraMonitorItemの場合のみ非null。SAT視点や視点なしはnull)を追加し、
  カメラスタックは毎回 `mc.player.getItemInHand(activeHand)` で再取得(NBT持ちの
  `ItemStack` はキャッシュしない)。
- **デバウンス(必須)** — `InteractionKeyMappingTriggered` は左クリック押しっぱなしで毎tick、
  右クリック押しっぱなしで数tickごとに再発火するため、`attackLatch`/`useLatch` の2つの
  ラッチで最初の1回だけ処理し、対応するバニラキー(`keyAttack`/`keyUse`)が離されるまで
  再処理しない。ラッチ解除は `onClientTick` の毎tickチェックで行う(`isActive()` の状態に
  関わらず常時)。`isUseItem()` は左右両手ぶんそれぞれ発火するため、`MAIN_HAND` の時だけ
  処理して二重切替を防止。
- **選択状態の永続化** — 切替(または開いた時点でIDが `SelectedCameraId` と異なる場合)の
  たびに新規C2Sパケット `SelectCameraPacket`(`SaberSpecialPacket` の形をそのまま踏襲)を
  送信。サーバーは `hand`/`cameraId` をそのアイテムの実際のリストに対して再検証してから
  `SelectedCameraId` を書き込む(クライアントの申告を信用しない)。クライアント側の視点自体は
  パケットの往復を待たずに即時反映(楽観的更新)。
- **対象外** — `FeedManager`/モニターブロックのフィード機構、`SatelliteRemoteItem` 本体の
  挙動(クリックが無効化される点以外は変更なし)、`SecurityCameraBlock`、`CameraRegistry`
  (引き続きサーバー専用・本機能では未使用、ワールド横断のカメラ一覧機能は無し)は今回一切
  変更していない。
- **検証状況(正直な記載)** — `gradlew build` は成功。dev `runClient` を起動し、MOD ロード・
  `ModNetwork` へのパケット登録(`SelectCameraPacket` 追加込み)・イベント登録に異常なく
  タイトル画面まで到達することを確認済み。ただし本セッションにはMinecraftのゲーム内操作
  (マウスクリック・Vキー押下・スニーク等)を自動操作するGUI/デスクトップ操作ツールが
  無いため、**実際のゲームプレイ手順による対話的検証(複数カメラ登録・スニーク解除・
  視点切替・クリックデバウンス・破壊済みカメラのスキップ・64m圏外判定・再ログイン後の
  選択永続化など)はこのセッションでは実施できていない**。特にリスクが高いとされていた
  「スニーク右クリックが `useOn` に届くか」については `DoorKeyItem.useOn` という
  既存の実装(スニーク時のみ処理しPASSで抜ける形)がまさに同じ前提の上で既に本番稼働して
  いることをコード上で確認しており、設計上は妥当と判断しているが、実機での目視確認は
  次回プレイ時に行うこと。

## v0.9: 監視カメラの距離制限撤廃

- **方式(スペクテイト機構の流用)** — クライアントは自分の周囲(サーバーのビュー距離)外の
  チャンクを受信できず、範囲外チャンクパケットは `ClientChunkCache` が破棄する
  ("Ignoring chunk since it's not in the view range")ため、強制ロード(`setChunkForced`/
  チケット)だけでは遠隔カメラは映らない。バニラで唯一「任意視点の周囲チャンクを
  クライアントへ配信」できるのは `ServerPlayer#setCamera(Entity)`(/spectate と同じ機構。
  SecurityCraft の実績あり): setCamera がプレイヤーをカメラ位置へテレポートし、以降毎tick
  `ServerPlayer.tick()` が `absMoveTo` + `chunkSource.move` で追跡チャンクを視点へ追従させる。
  よって携帯モニター(Vキー)の CAM 視点をサーバー主導のこの方式へ移行し、**距離無制限**
  (同ディメンション内)とする。強制チャンクチケットは一切使わない=リークの余地なし。
  サーバー負荷は「その場所にプレイヤーが1人立っている」のと同等。
- **挙動変更(要注意)** — CAM 視聴中はプレイヤー本体がカメラ位置へ移動する(スペクテイト式)。
  終了時(スニーク/Vキー/カメラ破壊/被攻撃/ログアウト/サーバー停止)に元の位置へ復帰。
  被攻撃時はその一撃をキャンセルして視聴を強制終了(常時無敵にはしない)。死亡・リスポーン後は
  テレポート復帰しない(セッション破棄のみ)。SAT 視点は従来どおりクライアント完結で変更なし。
- **サーバー実装** — `event/RemoteCameraView`(SaberDeflection と同型の static サブスクライバ)が
  UUID→セッション(復帰位置・CameraEntity・選択ID)を管理。open/cycle の検証は
  CameraRegistry.idAt(チャンクロード不要)→ 採用候補のみ getBlockState(同期ロード1チャンク)の
  二段構え。CameraEntity は既存の EntityType(noSave/NoopRenderer)をサーバー側で addFreshEntity
  して setCamera に渡す。切替は新エンティティ生成→setCamera→旧 discard。選択IDは従来どおり
  `SelectedCameraId` へサーバーが書き込む。
- **パケット** — C2S 3種を ModNetwork へ追加(SelectCameraPacket と同型): OpenCameraViewPacket
  (hand+cameraId、-1=自動選択)/ CycleCameraViewPacket(±1)/ CloseCameraViewPacket。
  サーバーは hand・アイテム・リスト内ID・レジストリ・ブロック実在を全て再検証(クライアント無信用)。
- **クライアント** — `ClientCameraHooks` の CAM 経路はパケット送信のみに縮退(選択・距離判定・
  64m 定数 `MAX_VIEW_DISTANCE` を削除)。リモート視聴状態は `mc.getCameraEntity() instanceof
  CameraEntity`(ローカルSAT実体と区別)のエッジ検出で管理し、入力抑制・クリック切替ラッチ・
  アクションバーヒントを流用。視聴中の本体は描画しない(SAT・モニターキャプチャ時の自己描画は維持)。
  視聴中のカメラ破壊はサーバーが20tick毎に再検証して自動終了(将来拡張の「常時再検証」を本経路で解消)。
- **モニターブロック** — `FeedManager.MAX_CAMERA_DISTANCE`(64m)と `closerThan` 判定を削除。
  表示可能範囲は「クライアントがロード済みのチャンク」(概ね描画距離)まで拡大。完全遠隔の
  壁掛けフィードは setCamera が1プレイヤー1視点である上、複数同時オフスクリーン描画には
  ClientChunkCache への mixin+独自チャンク同期が必要(本プロジェクトは mixin 基盤なし)のため
  対象外として明記。
- **文言・文書** — camera_none_valid から 64m 表記を除去、guide.camera.1 更新、
  camera_view_lost 追加、未使用の camera_out_of_range を削除。README / MANUAL_JA の
  64ブロック制限記述を撤廃し本体移動の挙動を記載。
- **検証(mcdrive.ps1)** — ①近距離リグレッション(視聴→スニーク復帰→座標一致) ②500ブロック先の
  カメラ(グロウストーン目印がスクリーンショットに写ること・復帰座標一致) ③`/forceload query` が
  空+視聴中に強制終了→再入場で元位置 ④近1+遠2の3台をクリック巡回+MSPT確認+再ログインで
  選択ID維持 ⑤破壊済みスキップ/視聴中破壊で自動終了 ⑥視聴中被攻撃で強制帰還
  ⑦モニターブロック120m先ライブ表示/1000m先は消灯 ⑧`gradlew build` 成功。

## 将来拡張(v0.7以降の候補)

- カメラ視点表示中、現在視聴中のカメラの有効性(破壊/圏外)は視点を開いた時・切替時にしか
  チェックしていない。tick単位での常時再検証(視聴中にカメラが壊された/範囲外に出た場合の
  自動フォールバック)は未実装で、今後の課題(v0.8時点でドキュメント化のみ、対応は見送り)。
  → v0.9計画で本経路(RemoteCameraViewの20tick毎再検証)により解消予定。
- ヘリのホバリング高度維持
- 鍵の複製レシピ(同一ID)・マスターキー
