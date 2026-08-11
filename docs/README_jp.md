# Mob Life

Mob Life は、ワールド作成時に選んだ Mob の姿でその世界を遊ぶ Fabric Mod です。
見た目だけを変えるのではなく、移動、当たり判定、視点の高さ、視界、食事、装備、クラフト、睡眠、戦闘まで、その体に合わせて変わります。

English version: [README](../README.md)

## スクリーンショット

以下の画像は少し小さめに表示して、ページ全体が読みやすいようにしています。
それぞれの見出しで、どの状況の画像か分かるようにしています。

**ワールド作成時の姿選択**

<figure>
  <img src="images/mob-life-world-selection.png" alt="ワールド作成時の姿選択画面" width="640">
  <figcaption>ワールド作成時に表示される姿選択画面です。</figcaption>
</figure>

**変身後の三人称視点**

<figure>
  <img src="images/mob-life-third-person-view.png" alt="変身後の三人称視点" width="640">
  <figcaption>Mob の姿に変身したあとの三人称視点です。</figcaption>
</figure>

**インベントリとクラフトの変化**

<figure>
  <img src="images/mob-life-inventory-layout.png" alt="インベントリとクラフトの変化" width="640">
  <figcaption>姿に応じて変わるインベントリ画面です。</figcaption>
</figure>

**視界の加工表現**

<figure>
  <img src="images/mob-life-dichromatic-vision.png" alt="色味が変わる視界の例" width="640">
  <figcaption>Mob Life の視界加工と色味の変化の例です。</figcaption>
</figure>

## できること

- 新規ワールド作成時に、最初の姿を選べます。
- 選んだ姿はワールドに保存され、その世界の前提になります。
- 初期スポーンは、選んだ姿が自然に出られる場所に調整されます。
- プレイヤー、牛、ヒツジ、ニワトリ、ネコ、オセロット、オオカミ、ブタ、ウマ、ロバ、ラバ、ウサギに対応しています。
- 姿に合わせて、サイズ、目線の高さ、移動感覚、鳴き声、見た目の色味が変わります。
- 姿ごとに、持てる物の量や使える装備枠も変わります。
- データパックで各姿の挙動を調整できます。
- `config/mob_life.json` で全体設定を切り替えられます。Mod Menu が入っていれば、GUI からも変更できます。

## 他の変身系 Mod との違い

Mob Life は、Mob の見た目を借りるだけの Mod ではありません。
その姿でどう遊ぶかを作る Mod です。

一般的な変身系 Mod は、見た目の差し替えや一部の能力コピーが中心になりがちです。Mob Life はそこから踏み込んで、姿ごとの移動、ジャンプ、届く距離、採掘、インベントリ、装備、食事、睡眠、視界、戦闘までまとめて設計しています。

もうひとつの違いは、ワールド作成時の選択を起点にしていることです。
最初に決めた姿がその世界の前提になり、あとから `/moblife morph` で切り替えることもできますが、基本は「その世界で何として生きるか」を先に決める作りです。

## 遊び方

- 新規ワールド作成時に姿を選びます。
- その姿に合った移動、視界、食事、装備、クラフトで遊びます。
- 別の姿を試したいときは `/moblife morph` で切り替えます。
- 細かな調整はデータパックや `config/mob_life.json` で行います。

## 姿一覧

最初に選べる姿は次のとおりです。

- 通常のプレイヤー
- 牛
- ヒツジ
- ニワトリ
- ネコ
- オセロット
- オオカミ
- ブタ
- ウマ
- ロバ
- ラバ
- ウサギ

## 設定

個別の姿ごとの基本設定は `data/mob_life/mob_life/morphs/<mob>.json` にあります。
データパックで同じパスを上書きすると、各姿の移動、視界、食事、戦闘、インベントリ、睡眠、特性を調整できます。
`/reload` を実行すると、変更が再読込されます。

全体設定は `config/mob_life.json` です。
ここでは通常プレイヤー姿の扱い、描画の処理、インベントリ制限、採掘や届く距離、違和感のデバッグ表示を切り替えられます。

## 動作環境

- Minecraft 26.2
- Fabric Loader 0.19.3
- Java 25
- Fabric API

## License

Mob Life is released under the [MIT License](../LICENSE).
