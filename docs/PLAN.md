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

## 将来拡張(v0.2以降の候補)

- 監視カメラの距離制限撤廃(サーバー側チャンクロード+カメラ同期パケット)
- モニターGUIで複数カメラ切替
- ミサイルのターゲット指定アイテム(レーザーデジグネーター)
- ドローンの装備/アップグレードシステム
