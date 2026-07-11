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

## 将来拡張(v0.7以降の候補)

- 監視カメラの距離制限撤廃(クライアントに遠隔チャンクを配信する仕組みが必要:
  偽装プレイヤー/チャンク同期パケットの大掛かりな設計になるため見送り中)
- ヘリのホバリング高度維持
- 鍵の複製レシピ(同一ID)・マスターキー
