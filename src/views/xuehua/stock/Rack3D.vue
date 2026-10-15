<template>
  <Teleport to="body">
    <div class="r3d-overlay">
      <!-- 顶栏：标题 / 货架切换 / 操作提示 / 关闭 -->
      <div class="r3d-bar">
        <div class="r3d-title">🧊 货架 3D 视图 <span v-if="targetBin" class="r3d-target-tip">目标：{{ targetBin.binCode }}</span></div>
        <div class="r3d-ops">
          <select v-model="rack" class="r3d-sel">
            <option v-for="r in racks" :key="r" :value="r">{{ r }} 货架</option>
          </select>
          <span class="r3d-hint">拖拽旋转 · 滚轮缩放 · 点击库位查看详情</span>
          <button class="r3d-close" @click="$emit('close')">✕ 关闭</button>
        </div>
      </div>

      <!-- 舞台 -->
      <div ref="stageRef" class="r3d-stage"></div>

      <!-- 右上角图例（原型 .r3d-legend 同款色） -->
      <div class="r3d-legend">
        <div class="lg lg-empty">空位</div>
        <div class="lg lg-full">在库</div>
        <div class="lg lg-pick">拣配中</div>
        <div class="lg lg-alarm">报警</div>
        <div class="lg lg-big">超大件</div>
        <div class="lg lg-target">目标库位</div>
      </div>

      <!-- 点击库位信息浮层 -->
      <div v-if="tip" class="r3d-tip" :style="{ left: tip.x + 'px', top: tip.y + 'px' }">
        <b>{{ tip.bin.binCode }}</b>
        <span>{{ tip.bin.partCode || '空位' }} · 数量 {{ tip.bin.qty ?? 0 }}</span>
        <span>状态：{{ tip.label }}</span>
        <span>WCS 地址：{{ tip.bin.rackCode }}-L{{ tip.bin.layer || 1 }}-C{{ tip.bin.colNo || 1 }}</span>
      </div>
    </div>
  </Teleport>
</template>

<script lang="ts" name="xuehua-stock-rack3d" setup>
  import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
  import type { BinRow } from './stock.api';

  const props = defineProps<{ bins: BinRow[]; initialRack?: string; targetBin?: BinRow | null }>();
  const emit = defineEmits(['close']);

  const stageRef = ref<HTMLDivElement | null>(null);
  const tip = ref<{ x: number; y: number; bin: BinRow; label: string } | null>(null);

  /** 货架清单（按 rack_code 分组） */
  const racks = computed(() => Array.from(new Set(props.bins.map((b) => b.rackCode || '?'))).sort());
  const rack = ref(props.initialRack || racks.value[0] || '');

  // Three.js 全局对象（index.html 引入 /vendor/three.min.js，原型同用法）
  let renderer: any = null;
  let scene: any = null;
  let camera: any = null;
  let raf = 0;
  let clickables: any[] = [];
  let targetMesh: any = null;

  // 手写轨道控制状态（原型同款：theta/phi/radius）
  const orbit = { theta: Math.PI / 5, phi: Math.PI / 3.2, radius: 9, dragging: false, lx: 0, ly: 0 };

  const ST_COLORS: Record<number, number> = { 0: 0xb5bec8, 1: 0x2cc873, 2: 0xf5a623, 3: 0xe74c3c, 4: 0x8e44ad };
  const ST_LABELS: Record<number, string> = { 0: '空位', 1: '在库', 2: '拣配中', 3: '报警', 4: '超大件存放' };

  function statusOf(b: BinRow): number {
    // 低库存（低于备件安全库存）在 3D 中同地图口径显示报警红
    if ((b as any).lowStock) return 3;
    return b.status ?? 0;
  }

  /** 构建单货架场景：4 立柱 + 层横梁 + 每层按真实 col_no 盒子 */
  function buildScene() {
    const THREE = (window as any).THREE;
    if (!renderer) return;
    // 清空旧场景对象
    while (scene.children.length) {
      const obj = scene.children[0];
      scene.remove(obj);
      obj.traverse?.((o: any) => {
        o.geometry?.dispose?.();
        if (o.material) Array.isArray(o.material) ? o.material.forEach((m: any) => m.dispose()) : o.material.dispose();
      });
    }
    clickables = [];
    targetMesh = null;

    // 灯光必须在清空后重建（Lambert 材质无光照渲染为纯黑——首版黑屏的根因）
    scene.add(new THREE.AmbientLight(0xffffff, 0.75));
    const dir = new THREE.DirectionalLight(0xffffff, 0.7);
    dir.position.set(6, 10, 8);
    scene.add(dir);

    const group = new THREE.Group();
    const rackBins = props.bins.filter((b) => (b.rackCode || '?') === rack.value);
    const L = Math.max(1, ...rackBins.map((b) => b.layer || 1));
    const C = Math.max(1, ...rackBins.map((b) => b.colNo || 1));
    const bw = 1.0,
      bh = 1.0,
      bd = 1.0,
      gap = 0.12;
    const W = C * (bw + gap),
      H = L * (bh + gap) + 0.3;

    // 立柱（4 根）与横梁
    const postGeo = new THREE.BoxGeometry(0.08, H, 0.08);
    const postMat = new THREE.MeshLambertMaterial({ color: 0x5a6b7d });
    for (const [px, pz] of [
      [-W / 2 - 0.1, bd / 2 + 0.06],
      [W / 2 + 0.1, bd / 2 + 0.06],
      [-W / 2 - 0.1, -bd / 2 - 0.06],
      [W / 2 + 0.1, -bd / 2 - 0.06],
    ]) {
      const post = new THREE.Mesh(postGeo, postMat);
      post.position.set(px, H / 2, pz);
      group.add(post);
    }
    const beamGeo = new THREE.BoxGeometry(W + 0.3, 0.06, bd + 0.16);
    for (let l = 0; l <= L; l++) {
      const beam = new THREE.Mesh(beamGeo, postMat);
      beam.position.set(0, 0.15 + l * (bh + gap), 0);
      group.add(beam);
    }

    // 库位盒子（真实状态色；拣配中自发光；目标位橙红脉动+编号镶面）
    const tBin = props.targetBin;
    for (const b of rackBins) {
      const st = statusOf(b);
      const isTarget = tBin && b.binCode === tBin.binCode;
      const fillH = st === 0 ? 0.25 : 0.55 + Math.min(0.35, ((b.qty || 1) % 10) / 20);
      const geo = new THREE.BoxGeometry(bw - 0.08, bh * fillH, bd - 0.12);
      const mat = new THREE.MeshLambertMaterial({
        color: isTarget ? 0xff5a2e : ST_COLORS[st] ?? 0xb5bec8,
        emissive: isTarget ? 0x882200 : st === 2 ? 0x553300 : 0x000000,
      });
      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.set(-(W - bw - gap) / 2 + ((b.colNo || 1) - 1) * (bw + gap), 0.15 + ((b.layer || 1) - 0.5) * (bh + gap), 0);
      (mesh as any).userData = { bin: b };
      group.add(mesh);
      clickables.push(mesh);
      if (isTarget) {
        targetMesh = mesh;
        // 库位号 Sprite 镶面（原型同款）
        const cv = document.createElement('canvas');
        cv.width = 256;
        cv.height = 64;
        const ctx = cv.getContext('2d')!;
        ctx.fillStyle = 'rgba(0,0,0,.55)';
        ctx.fillRect(0, 0, 256, 64);
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 34px Consolas';
        ctx.textAlign = 'center';
        ctx.fillText(b.binCode, 128, 44);
        const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: new THREE.CanvasTexture(cv), transparent: true }));
        sprite.scale.set(1.6, 0.4, 1);
        sprite.position.set(mesh.position.x, mesh.position.y + 0.62, 0.2);
        group.add(sprite);
      }
    }

    // 地面
    const floor = new THREE.Mesh(
      new THREE.PlaneGeometry(W + 4, 6),
      new THREE.MeshLambertMaterial({ color: 0xdfe6ee })
    );
    floor.rotation.x = -Math.PI / 2;
    group.add(floor);

    // 货架号标签
    const labelCv = document.createElement('canvas');
    labelCv.width = 256;
    labelCv.height = 64;
    const lctx = labelCv.getContext('2d')!;
    lctx.fillStyle = '#1d5fa8';
    lctx.fillRect(0, 0, 256, 64);
    lctx.fillStyle = '#fff';
    lctx.font = 'bold 38px Microsoft YaHei';
    lctx.textAlign = 'center';
    lctx.fillText(`${rack.value} 货架`, 128, 46);
    const label = new THREE.Sprite(new THREE.SpriteMaterial({ map: new THREE.CanvasTexture(labelCv) }));
    label.scale.set(2.4, 0.6, 1);
    label.position.set(0, H + 0.6, 0);
    group.add(label);

    scene.add(group);
    fitCamera();
  }

  function fitCamera() {
    const rackBins = props.bins.filter((b) => (b.rackCode || '?') === rack.value);
    const L = Math.max(1, ...rackBins.map((b) => b.layer || 1));
    orbit.radius = Math.max(7, Math.min(26, L * 3.2));
  }

  function updateCamera() {
    if (!camera) return;
    camera.position.set(
      orbit.radius * Math.sin(orbit.phi) * Math.cos(orbit.theta),
      orbit.radius * Math.cos(orbit.phi) + L_Height() / 2,
      orbit.radius * Math.sin(orbit.phi) * Math.sin(orbit.theta)
    );
    camera.lookAt(0, L_Height() / 2, 0);
  }
  function L_Height(): number {
    const rackBins = props.bins.filter((b) => (b.rackCode || '?') === rack.value);
    return Math.max(1, ...rackBins.map((b) => b.layer || 1)) * 1.12 + 0.3;
  }

  function animate() {
    raf = requestAnimationFrame(animate);
    if (targetMesh) {
      // 目标位脉动（原型同款）
      const t = Date.now() / 500;
      targetMesh.material.emissive.setHex(Math.sin(t) > 0 ? 0x882200 : 0x220800);
    }
    if (renderer) renderer.render(scene, camera);
  }

  function onDown(e: PointerEvent) {
    orbit.dragging = true;
    orbit.lx = e.clientX;
    orbit.ly = e.clientY;
  }
  function onMove(e: PointerEvent) {
    if (!orbit.dragging) return;
    orbit.theta += (e.clientX - orbit.lx) * 0.008;
    orbit.phi = Math.max(0.15, Math.min(Math.PI / 2.05, orbit.phi - (e.clientY - orbit.ly) * 0.006));
    orbit.lx = e.clientX;
    orbit.ly = e.clientY;
    updateCamera();
  }
  function onUp() {
    orbit.dragging = false;
  }
  function onWheel(e: WheelEvent) {
    e.preventDefault();
    orbit.radius = Math.max(4, Math.min(26, orbit.radius + (e.deltaY > 0 ? 0.8 : -0.8)));
    updateCamera();
  }
  function onClick(e: MouseEvent) {
    const THREE = (window as any).THREE;
    if (!renderer || !clickables.length) return;
    const rect = renderer.domElement.getBoundingClientRect();
    const ndc = new THREE.Vector2(((e.clientX - rect.left) / rect.width) * 2 - 1, -((e.clientY - rect.top) / rect.height) * 2 + 1);
    const ray = new THREE.Raycaster();
    ray.setFromCamera(ndc, camera);
    const hits = ray.intersectObjects(clickables);
    if (hits.length) {
      const bin: BinRow = hits[0].object.userData.bin;
      tip.value = { x: e.clientX, y: Math.max(60, e.clientY - 90), bin, label: ST_LABELS[statusOf(bin)] ?? '未知' };
      setTimeout(() => (tip.value = null), 4000);
    }
  }
  function onKey(e: KeyboardEvent) {
    if (e.key === 'Escape') emit('close');
  }

  onMounted(() => {
    const THREE = (window as any).THREE;
    const stage = stageRef.value;
    if (!THREE || !stage) return;
    try {
      renderer = new THREE.WebGLRenderer({ antialias: true });
      renderer.setSize(stage.clientWidth, stage.clientHeight);
      renderer.setPixelRatio(window.devicePixelRatio);
      stage.appendChild(renderer.domElement);
      scene = new THREE.Scene();
      scene.background = new THREE.Color(0x0e1622);
      scene.fog = new THREE.Fog(0x0e1622, 18, 40);
      camera = new THREE.PerspectiveCamera(50, stage.clientWidth / stage.clientHeight, 0.1, 100);

      buildScene();
      updateCamera();
      animate();

      stage.addEventListener('pointerdown', onDown);
      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
      stage.addEventListener('wheel', onWheel, { passive: false });
      renderer.domElement.addEventListener('click', onClick);
      window.addEventListener('keydown', onKey);
    } catch (err) {
      // WebGL 不可用降级：给出提示不阻断
      stage.innerHTML = '<div style="color:#9fb8d4;padding:60px;text-align:center">当前环境不支持 WebGL，无法渲染 3D 货架（平面地图不受影响）</div>';
    }
  });

  onBeforeUnmount(() => {
    cancelAnimationFrame(raf);
    window.removeEventListener('pointermove', onMove);
    window.removeEventListener('pointerup', onUp);
    window.removeEventListener('keydown', onKey);
    renderer?.dispose?.();
    renderer = null;
    scene = null;
    camera = null;
  });

  watch(rack, buildScene);
</script>

<style lang="less" scoped>
  /* 原型 .r3d-* 全屏查看器样式 */
  .r3d-overlay {
    position: fixed;
    inset: 0;
    z-index: 999;
    background: #0e1622;
    display: flex;
    flex-direction: column;
  }

  .r3d-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 16px;
    background: #101d30;
    border-bottom: 1px solid rgba(90, 160, 230, 0.25);

    .r3d-title {
      color: #eaf6ff;
      font-size: 15px;
      font-weight: 700;

      .r3d-target-tip {
        margin-left: 10px;
        padding: 2px 10px;
        font-size: 12px;
        color: #ffd7c2;
        background: rgba(255, 90, 46, 0.18);
        border: 1px solid rgba(255, 90, 46, 0.5);
        border-radius: 10px;
      }
    }

    .r3d-ops {
      display: flex;
      align-items: center;
      gap: 12px;

      .r3d-sel {
        padding: 5px 10px;
        border-radius: 6px;
        border: 1px solid rgba(120, 180, 230, 0.4);
        background: #16273d;
        color: #cfe3f5;
      }

      .r3d-hint {
        font-size: 12px;
        color: #6f93b8;
      }

      .r3d-close {
        padding: 5px 12px;
        border-radius: 6px;
        border: 1px solid rgba(255, 107, 125, 0.5);
        background: rgba(231, 76, 60, 0.15);
        color: #ff9a8b;
        cursor: pointer;

        &:hover {
          background: rgba(231, 76, 60, 0.35);
        }
      }
    }
  }

  .r3d-stage {
    flex: 1;
    overflow: hidden;
    cursor: grab;

    &:active {
      cursor: grabbing;
    }
  }

  .r3d-legend {
    position: absolute;
    top: 64px;
    right: 16px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 10px 12px;
    background: rgba(10, 20, 35, 0.8);
    border: 1px solid rgba(90, 160, 230, 0.25);
    border-radius: 8px;

    .lg {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #cfe3f5;
      font-size: 12px;

      &::before {
        content: '';
        width: 12px;
        height: 12px;
        border-radius: 3px;
      }
    }

    .lg-empty::before { background: #b5bec8; }
    .lg-full::before { background: #2cc873; }
    .lg-pick::before { background: #f5a623; }
    .lg-alarm::before { background: #e74c3c; }
    .lg-big::before { background: #8e44ad; }
    .lg-target::before { background: #ff5a2e; box-shadow: 0 0 8px #ff5a2e; }
  }

  .r3d-tip {
    position: fixed;
    z-index: 1000;
    display: flex;
    flex-direction: column;
    gap: 3px;
    padding: 10px 14px;
    background: rgba(8, 16, 28, 0.92);
    border: 1px solid rgba(0, 224, 255, 0.45);
    border-radius: 8px;
    color: #cfe3f5;
    font-size: 12px;
    pointer-events: none;

    b {
      color: #7fd8ff;
      font-size: 14px;
    }
  }
</style>
