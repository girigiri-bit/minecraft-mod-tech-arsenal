# Tech Arsenal

Minecraft 1.20.1 (Forge) 向けテクノロジーMOD集。近未来的な監視・攻撃テクノロジーをMinecraftに追加します。

## 収録テクノロジー (v0.1)

- **衛星カメラ (Satellite Camera)** — 衛星リモコン使用で真上80ブロックから見下ろす衛星視点に切替。スニークで解除
- **監視カメラ (Security Camera)** — 設置型カメラブロック。カメラモニターでリンクし、遠隔(64ブロック以内)からカメラ視点を確認
- **誘導ミサイル (Guided Missile)** — ミサイルランチャーで視線方向のモンスターをロックオンして発射。追尾して爆発(ブロック破壊なし)
- **戦闘ドローン (Combat Drone)** — 召喚するとオーナーに追従して飛行し、モンスターを自動索敵・射撃。スニーク右クリックで回収

実装の詳細は [docs/PLAN.md](docs/PLAN.md) を参照。

## 開発環境

| 項目 | バージョン |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.10 |
| Java | 17 |
| Mod ID | `techarsenal` |

## ビルド方法

```
.\gradlew.bat build
```

成果物は `build/libs/` に出力されます。

## クライアント起動(開発用)

```
.\gradlew.bat runClient
```

## ライセンス

MIT
