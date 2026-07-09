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

## 将来拡張(v0.5以降の候補)

- 監視カメラの距離制限撤廃(クライアントに遠隔チャンクを配信する仕組みが必要:
  偽装プレイヤー/チャンク同期パケットの大掛かりな設計になるため見送り中)
- ビークル搭載武器の弾薬化・ヘリのホバリング高度維持
- 火炎放射器の燃料システム
