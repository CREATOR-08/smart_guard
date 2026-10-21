import React, { useState, useRef, useEffect } from 'react';

// const API_URL = 'https://smart-guard-hpze.onrender.com/api/polygon/analyze';
const API_URL='https://smart-guard-azure.vercel.app/api/polygon/analyze';

const PRESETS = {
  complex: [
    { x: 90, y: 90 }, { x: 300, y: 70 }, { x: 340, y: 190 }, { x: 470, y: 80 }, { x: 650, y: 120 },
    { x: 620, y: 390 }, { x: 430, y: 300 }, { x: 320, y: 400 }, { x: 190, y: 290 }, { x: 90, y: 380 }
  ],
  maze: [
    { x: 50, y: 50 }, { x: 730, y: 50 }, { x: 730, y: 120 }, { x: 120, y: 120 },
    { x: 120, y: 190 }, { x: 730, y: 190 }, { x: 730, y: 260 }, { x: 120, y: 260 },
    { x: 120, y: 330 }, { x: 730, y: 330 }, { x: 730, y: 400 }, { x: 50, y: 400 }
  ],
  verticalMaze: [
    { x: 70, y: 55 }, { x: 710, y: 55 }, { x: 710, y: 405 }, { x: 640, y: 405 },
    { x: 640, y: 125 }, { x: 570, y: 125 }, { x: 570, y: 405 }, { x: 500, y: 405 },
    { x: 500, y: 125 }, { x: 430, y: 125 }, { x: 430, y: 405 }, { x: 360, y: 405 },
    { x: 360, y: 125 }, { x: 290, y: 125 }, { x: 290, y: 405 }, { x: 220, y: 405 },
    { x: 220, y: 125 }, { x: 150, y: 125 }, { x: 150, y: 405 }, { x: 70, y: 405 }
  ],
  labyrinth: [
    { x: 190, y: 30 }, { x: 590, y: 30 }, { x: 590, y: 430 }, { x: 550, y: 430 },
    { x: 550, y: 90 }, { x: 510, y: 90 }, { x: 510, y: 380 }, { x: 470, y: 380 },
    { x: 470, y: 130 }, { x: 430, y: 130 }, { x: 430, y: 340 }, { x: 390, y: 340 },
    { x: 390, y: 90 }, { x: 350, y: 90 }, { x: 350, y: 390 }, { x: 310, y: 390 },
    { x: 310, y: 150 }, { x: 270, y: 150 }, { x: 270, y: 430 }, { x: 230, y: 430 },
    { x: 230, y: 110 }, { x: 190, y: 110 }
  ],
  comb: [
    { x: 50, y: 350 }, { x: 100, y: 100 }, { x: 150, y: 350 }, { x: 250, y: 100 },
    { x: 300, y: 350 }, { x: 400, y: 100 }, { x: 450, y: 350 }, { x: 550, y: 100 },
    { x: 600, y: 350 }, { x: 700, y: 100 }, { x: 750, y: 350 }, { x: 750, y: 420 }, { x: 50, y: 420 }
  ],
  star: [
    { x: 390, y: 50 }, { x: 450, y: 180 }, { x: 600, y: 180 }, { x: 480, y: 270 },
    { x: 530, y: 400 }, { x: 390, y: 320 }, { x: 250, y: 400 }, { x: 300, y: 270 },
    { x: 180, y: 180 }, { x: 330, y: 180 }
  ]
};

const PRESET_LABELS = {
  complex: 'Angular Polygon',
  maze: 'Horizontal Maze',
  verticalMaze: 'Vertical Maze',
  labyrinth: 'Square Labyrinth',
  comb: 'Worst-Case Comb',
  star: 'Star Polygon'
};
const COLOR_HEX = ['#fb7185', '#34d399', '#818cf8'];
const COLOR_TINTS = ['rgba(251, 113, 133, 0.12)', 'rgba(52, 211, 153, 0.12)', 'rgba(129, 140, 248, 0.14)'];
const TRIANGLE_TINTS = ['rgba(34, 211, 238, 0.12)', 'rgba(45, 212, 191, 0.12)', 'rgba(129, 140, 248, 0.13)'];
const COLOR_NAMES = ['Color 1 (Coral)', 'Color 2 (Mint)', 'Color 3 (Violet)'];
const VERTEX_TYPE_COLORS = {
  START: '#38bdf8',
  END: '#34d399',
  SPLIT: '#fb7185',
  MERGE: '#fbbf24',
  REGULAR: '#cbd5e1'
};

export default function App() {
  const canvasRef = useRef(null);
  const [vertices, setVertices] = useState([]);
  const [isClosed, setIsClosed] = useState(false);
  const [mousePos, setMousePos] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [step, setStep] = useState('DRAW');
  const [visibleTrianglesCount, setVisibleTrianglesCount] = useState(0);
  const [cameraMode, setCameraMode] = useState('GREEDY');
  const [errorMsg, setErrorMsg] = useState(null);
  const [activePreset, setActivePreset] = useState(null);
  const [sweepLineY, setSweepLineY] = useState(null);
  const [sweepCompleted, setSweepCompleted] = useState(false);
  const [sweepRun, setSweepRun] = useState(0);

  useEffect(() => {
    if (step !== 'SWEEP_LINE' || !analysis?.valid || vertices.length === 0) return undefined;

    const minY = Math.min(...vertices.map(({ y }) => y));
    const maxY = Math.max(...vertices.map(({ y }) => y));
    const duration = 5000;
    const startY = minY - 20;
    const endY = maxY + 20;
    let animationFrame;
    let startedAt;

    setSweepCompleted(false);
    setSweepLineY(startY);

    const animateSweep = (timestamp) => {
      if (startedAt === undefined) startedAt = timestamp;
      const progress = Math.min((timestamp - startedAt) / duration, 1);
      setSweepLineY(startY + (endY - startY) * progress);

      if (progress < 1) {
        animationFrame = requestAnimationFrame(animateSweep);
      } else {
        setSweepCompleted(true);
      }
    };

    animationFrame = requestAnimationFrame(animateSweep);
    return () => cancelAnimationFrame(animationFrame);
  }, [step, analysis, vertices, sweepRun]);

  useEffect(() => {
    if (step === 'TRIANGULATE' && analysis?.triangles) {
      setVisibleTrianglesCount(0);
      let current = 0;
      const total = analysis.triangles.length;
      const timer = setInterval(() => {
        current += 1;
        setVisibleTrianglesCount(current);
        if (current >= total) clearInterval(timer);
      }, 700);
      return () => clearInterval(timer);
    } else if (analysis?.triangles) {
      setVisibleTrianglesCount(analysis.triangles.length);
    }
  }, [step, analysis]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = '#1e293b';
    ctx.lineWidth = 1;
    for (let x = 0; x < canvas.width; x += 40) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
    }
    for (let y = 0; y < canvas.height; y += 40) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
    }

    if (vertices.length === 0) return;

    if (step === 'CAMERAS' && analysis?.valid) {
      ctx.save();
      ctx.beginPath();
      vertices.forEach((v, idx) => {
        if (idx === 0) ctx.moveTo(v.x, v.y);
        else ctx.lineTo(v.x, v.y);
      });
      ctx.closePath();
      ctx.fillStyle = 'rgba(56, 189, 248, 0.22)';
      ctx.fill();
      ctx.restore();
    }

    if ((step === 'TRIANGULATE' || step === 'COLOR' || step === 'CAMERAS') && analysis?.triangles) {
      const count = step === 'TRIANGULATE' ? visibleTrianglesCount : analysis.triangles.length;
      for (let i = 0; i < count; i++) {
        const t = analysis.triangles[i];
        const p1 = vertices[t.v1];
        const p2 = vertices[t.v2];
        const p3 = vertices[t.v3];

        ctx.beginPath();
        ctx.moveTo(p1.x, p1.y);
        ctx.lineTo(p2.x, p2.y);
        ctx.lineTo(p3.x, p3.y);
        ctx.closePath();

        const isCurrentTriangle = step === 'TRIANGULATE' && i === count - 1;
        const colorIndex = analysis?.vertexColors?.[t.v1] ?? i % TRIANGLE_TINTS.length;
        ctx.fillStyle = isCurrentTriangle
          ? 'rgba(250, 204, 21, 0.3)'
          : step === 'COLOR'
            ? COLOR_TINTS[colorIndex]
            : TRIANGLE_TINTS[i % TRIANGLE_TINTS.length];
        ctx.fill();

        ctx.strokeStyle = isCurrentTriangle ? '#fbbf24' : '#2dd4bf';
        ctx.setLineDash([6, 4]);
        ctx.lineWidth = isCurrentTriangle ? 2 : 1.25;
        ctx.stroke();
        ctx.setLineDash([]);
      }
    }

    ctx.beginPath();
    vertices.forEach((v, idx) => {
      if (idx === 0) ctx.moveTo(v.x, v.y);
      else ctx.lineTo(v.x, v.y);
    });
    if (isClosed) {
      ctx.closePath();
      ctx.strokeStyle = analysis?.valid === false ? '#fb7185' : '#5eead4';
    } else {
      ctx.strokeStyle = '#94a3b8';
    }
    ctx.lineWidth = 2.5;
    ctx.stroke();

    if (!isClosed && mousePos && vertices.length > 0) {
      const last = vertices[vertices.length - 1];
      ctx.beginPath();
      ctx.moveTo(last.x, last.y);
      ctx.lineTo(mousePos.x, mousePos.y);
      ctx.strokeStyle = '#64748b';
      ctx.setLineDash([4, 4]);
      ctx.stroke();
      ctx.setLineDash([]);
    }

    if (step === 'SWEEP_LINE' && analysis?.valid && sweepLineY !== null) {
      ctx.save();
      ctx.beginPath();
      vertices.forEach((v, idx) => {
        if (idx === 0) ctx.moveTo(v.x, v.y);
        else ctx.lineTo(v.x, v.y);
      });
      ctx.closePath();
      ctx.clip();
      ctx.beginPath();
      ctx.moveTo(0, sweepLineY);
      ctx.lineTo(canvas.width, sweepLineY);
      ctx.strokeStyle = 'rgba(45, 212, 191, 0.28)';
      ctx.lineWidth = 12;
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(0, sweepLineY);
      ctx.lineTo(canvas.width, sweepLineY);
      ctx.strokeStyle = '#5eead4';
      ctx.lineWidth = 2;
      ctx.shadowColor = '#2dd4bf';
      ctx.shadowBlur = 12;
      ctx.stroke();
      ctx.restore();
    }

    const activeCameras = step === 'CAMERAS' && analysis?.valid
      ? (cameraMode === 'GREEDY' ? analysis.greedySetCoverCameras : analysis.artGalleryCameras)
      : [];

    vertices.forEach((v, idx) => {
      const isCamera = activeCameras.includes(idx);
      const vType = analysis?.vertexTypes ? analysis.vertexTypes[idx] : 'REGULAR';
      const isClassified = analysis?.valid && (
        (step === 'SWEEP_LINE' && sweepLineY !== null && sweepLineY >= v.y) ||
        (sweepCompleted && (step === 'FINISHED' || step === 'TRIANGULATE'))
      );
      
      ctx.beginPath();

      if (isClassified && !isCamera) {
        if (vType === 'START') {
          ctx.moveTo(v.x, v.y - 9); ctx.lineTo(v.x - 8, v.y + 7); ctx.lineTo(v.x + 8, v.y + 7);
          ctx.fillStyle = VERTEX_TYPE_COLORS.START;
        } else if (vType === 'END') {
          ctx.moveTo(v.x, v.y + 9); ctx.lineTo(v.x - 8, v.y - 7); ctx.lineTo(v.x + 8, v.y - 7);
          ctx.fillStyle = VERTEX_TYPE_COLORS.END;
        } else if (vType === 'SPLIT') {
          ctx.moveTo(v.x, v.y - 10); ctx.lineTo(v.x - 8, v.y); ctx.lineTo(v.x, v.y + 10); ctx.lineTo(v.x + 8, v.y);
          ctx.fillStyle = VERTEX_TYPE_COLORS.SPLIT;
        } else if (vType === 'MERGE') {
          ctx.rect(v.x - 7, v.y - 7, 14, 14);
          ctx.fillStyle = VERTEX_TYPE_COLORS.MERGE;
        } else {
          ctx.arc(v.x, v.y, 7, 0, Math.PI * 2);
          ctx.fillStyle = VERTEX_TYPE_COLORS.REGULAR;
        }
      } else {
        // Draw standard circles once triangulation is done (or for cameras)
        ctx.arc(v.x, v.y, isCamera ? 12 : 7, 0, Math.PI * 2);
        if (step === 'COLOR' || step === 'CAMERAS') {
          ctx.fillStyle = COLOR_HEX[analysis?.vertexColors?.[idx] ?? 0];
        } else {
          ctx.fillStyle = '#f8fafc';
        }
      }

      ctx.fill();
      ctx.lineWidth = 1.5;
      ctx.strokeStyle = '#0f172a';
      ctx.closePath();
      ctx.stroke();

      if (isCamera) {
        ctx.beginPath();
        ctx.arc(v.x, v.y, 20, 0, Math.PI * 2);
        ctx.strokeStyle = '#facc15';
        ctx.lineWidth = 2.5;
        ctx.stroke();
        ctx.font = '16px sans-serif';
        ctx.fillText('📷', v.x - 10, v.y - 16);
      }

      ctx.font = 'bold 12px Inter, sans-serif';
      ctx.fillStyle = '#e2e8f0';
      ctx.fillText(`P${idx + 1}`, v.x + 12, v.y + 6);
    });
  }, [vertices, isClosed, mousePos, step, analysis, visibleTrianglesCount, cameraMode, sweepLineY, sweepCompleted]);

  const handleCanvasClick = (e) => {
    if (isClosed) return;
    const rect = canvasRef.current.getBoundingClientRect();
    const x = Math.round((e.clientX - rect.left) * (canvasRef.current.width / rect.width));
    const y = Math.round((e.clientY - rect.top) * (canvasRef.current.height / rect.height));
    setVertices((prev) => [...prev, { x, y }]);
  };

  const handleMouseMove = (e) => {
    const rect = canvasRef.current.getBoundingClientRect();
    setMousePos({
      x: Math.round((e.clientX - rect.left) * (canvasRef.current.width / rect.width)),
      y: Math.round((e.clientY - rect.top) * (canvasRef.current.height / rect.height))
    });
  };

  const runBackendAnalysis = async (pts) => {
    setErrorMsg(null);
    setAnalysis(null);
    setIsClosed(false);
    setStep('FINISHED');
    setSweepLineY(null);
    setSweepCompleted(false);
    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(pts)
      });
      const data = await response.json();
      setAnalysis(data);
      setIsClosed(true);
      setStep('FINISHED');
    } catch (err) {
      setErrorMsg('Cannot reach backend. Ensure your API_URL is correct.');
    }
  };

  const handleFinishPolygon = () => {
    if (vertices.length < 3) return;
    runBackendAnalysis(vertices);
  };

  const loadPreset = (key) => {
    const pts = PRESETS[key];
    setActivePreset(key);
    setVertices(pts);
    runBackendAnalysis(pts);
  };

  const handleClear = () => {
    setActivePreset(null);
    setVertices([]);
    setIsClosed(false);
    setAnalysis(null);
    setStep('DRAW');
    setSweepLineY(null);
    setSweepCompleted(false);
    setErrorMsg(null);
  };

  const handleSweepLine = () => {
    setSweepCompleted(false);
    setSweepLineY(null);
    setStep('SWEEP_LINE');
    setSweepRun((currentRun) => currentRun + 1);
  };

  const handleResetPipeline = () => {
    setStep('FINISHED');
    setSweepLineY(null);
    setSweepCompleted(false);
  };

  const classifiedVertexCount = analysis?.valid
    ? vertices.filter((vertex) => (
      sweepCompleted ||
      (step === 'SWEEP_LINE' && sweepLineY !== null && sweepLineY >= vertex.y)
    )).length
    : 0;

  return (
    <div className="app-container">
      <header className="header">
        <h1>Art Gallery Problem — Camera Placement Visualizer</h1>
        <span style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
          Click canvas to place vertices (P1 → P2 → ... → Pn)
        </span>
      </header>

      <div className="main-layout">
        {/* LEFT SIDEBAR: Examples */}
        <div className="sidebar">
          <h3 className="section-title">Shape Library</h3>
          <p className="sidebar-hint">Load a shape to explore its triangulation.</p>
          {Object.entries(PRESETS).map(([key, points]) => (
            <button
              key={key}
              className={`preset-button ${activePreset === key ? 'selected' : ''}`}
              onClick={() => loadPreset(key)}
              aria-pressed={activePreset === key}
            >
              <svg className="preset-icon" viewBox="0 0 32 32" aria-hidden="true">
                <path d="M5 4v24h22V4H5Zm0 6h16v5H11v7h16M5 22h6" />
              </svg>
              <span className="preset-copy">
                <span>{PRESET_LABELS[key]}</span>
                <small>{points.length} corners</small>
              </span>
            </button>
          ))}
        </div>

        {/* CENTER: Canvas and Status */}
        <div className="center-content">
          <div className="canvas-actions">
            <button className="primary center-btn" onClick={handleFinishPolygon} disabled={isClosed || vertices.length < 3}>
              Finish Polygon
            </button>
            <button className="danger center-btn" onClick={handleClear}>
              Clear Canvas
            </button>
          </div>
          
          <div className="canvas-wrapper">
            <canvas
              ref={canvasRef}
              width={780}
              height={460}
              onClick={handleCanvasClick}
              onMouseMove={handleMouseMove}
            />
          </div>

          <div className="status-bar">
            <div><strong>Vertices:</strong> {vertices.length}</div>
            <div><strong>Edges:</strong> {isClosed ? vertices.length : Math.max(0, vertices.length - 1)}</div>
            <div>
              <strong>Status:</strong>{' '}
              {errorMsg ? (
                <span className="status-invalid">{errorMsg}</span>
              ) : !isClosed ? (
                <span>Drawing mode — click to add vertices</span>
              ) : analysis?.valid ? (
                <span className="status-valid">{analysis.statusMessage}</span>
              ) : (
                <span className="status-invalid">{analysis?.statusMessage}</span>
              )}
            </div>
          </div>

          {/* BOTTOM DETAILS */}
          <div className="bottom-details">
            {/* Vertices List */}
            <div className="card">
              <h3>Vertices Coordinates</h3>
              <div className="vertex-list">
                {vertices.length === 0 ? (
                  <div style={{ color: '#64748b' }}>No vertices placed yet.</div>
                ) : (
                  vertices.map((v, i) => (
                    <div key={i} className="vertex-row">
                      <span>P{i + 1} ({v.x}, {v.y})</span>
                      {analysis?.valid && (
                        <span style={{
                        fontSize: '0.75rem', 
                        color: '#08111d',
                        background: (
                          sweepCompleted ||
                          (step === 'SWEEP_LINE' && sweepLineY !== null && sweepLineY >= v.y)
                        ) ? VERTEX_TYPE_COLORS[analysis.vertexTypes?.[i] ?? 'REGULAR'] : '#475569',
                        padding: '2px 6px', 
                        borderRadius: '4px',
                        fontWeight: 'bold',
                        whiteSpace: 'nowrap'
                      }}>
                        {(
                          sweepCompleted ||
                          (step === 'SWEEP_LINE' && sweepLineY !== null && sweepLineY >= v.y)
                        ) ? analysis.vertexTypes?.[i] ?? 'REGULAR' : 'PENDING'}
                        </span>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* 3-Coloring Breakdown */}
            {(step === 'COLOR' || step === 'CAMERAS') && analysis?.valid && (
              <div className="card">
                <h3>3-Coloring Summary</h3>
                <div className="stat-row">
                  <span className="badge-red">🔴 Color 1:</span>
                  <span>{analysis.colorCounts[0]} vertices</span>
                </div>
                <div className="stat-row">
                  <span className="badge-green">🟢 Color 2:</span>
                  <span>{analysis.colorCounts[1]} vertices</span>
                </div>
                <div className="stat-row">
                  <span className="badge-blue">🔵 Color 3:</span>
                  <span>{analysis.colorCounts[2]} vertices</span>
                </div>
                <div className="stat-row" style={{ marginTop: '8px', borderTop: '1px solid #334155', paddingTop: '8px' }}>
                  <strong>Smallest Color Class:</strong>
                  <span>{COLOR_NAMES[analysis.smallestColorClass]}</span>
                </div>
              </div>
            )}

            {/* Camera Comparison */}
            {step === 'CAMERAS' && analysis?.valid && (
              <div className="card" style={{ gridColumn: '1 / -1' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                  <div>
                    <h3>Art Gallery Bound</h3>
                    <div className="stat-row">
                      <span>Theoretical Bound ⌊n/3⌋:</span>
                      <strong>{analysis.theoreticalUpperBound} cameras</strong>
                    </div>
                    <div className="stat-row">
                      <span>Smallest Color Class:</span>
                      <strong>{analysis.artGalleryCameras.length} cameras</strong>
                    </div>
                    <div className="stat-row">
                      <span>Vertices:</span>
                      <span>{analysis.artGalleryCameras.map((i) => `P${i + 1}`).join(', ')}</span>
                    </div>
                  </div>
                  
                  <div>
                    <h3>Greedy Set Cover</h3>
                    <div className="stat-row">
                      <span>Cameras Selected:</span>
                      <strong style={{ color: '#4ade80' }}>{analysis.greedySetCoverCameras.length} cameras</strong>
                    </div>
                    <div className="stat-row">
                      <span>Vertices:</span>
                      <span>{analysis.greedySetCoverCameras.map((i) => `P${i + 1}`).join(', ')}</span>
                    </div>
                    <div className="stat-row">
                      <span>Coverage:</span>
                      <strong style={{ color: '#38bdf8' }}>{analysis.coveragePercentage}%</strong>
                    </div>
                  </div>
                </div>

                <div className="mode-toggle">
                  <button className={cameraMode === 'GREEDY' ? 'primary' : ''} onClick={() => setCameraMode('GREEDY')}>
                    Show Greedy Selection
                  </button>
                  <button className={cameraMode === 'ART_GALLERY' ? 'primary' : ''} onClick={() => setCameraMode('ART_GALLERY')}>
                    Show 3-Coloring Selection
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* RIGHT SIDEBAR: Pipeline */}
        <div className="sidebar">
          <h3 className="section-title">Algorithm Pipeline</h3>
          <button className={`center-btn ${step === 'SWEEP_LINE' ? 'active-step' : ''}`} disabled={!analysis?.valid} onClick={handleSweepLine}>
            {sweepCompleted ? '1. Replay Sweep Line' : '1. Sweep Line &amp; Classify'}
          </button>
          <button className={`center-btn ${step === 'TRIANGULATE' ? 'active-step' : ''}`} disabled={!analysis?.valid} onClick={() => setStep('TRIANGULATE')}>
            2. Triangulate
          </button>
          <button className={`center-btn ${step === 'COLOR' ? 'active-step' : ''}`} disabled={!analysis?.valid || (step !== 'TRIANGULATE' && step !== 'COLOR' && step !== 'CAMERAS')} onClick={() => setStep('COLOR')}>
            3. 3-Color Graph
          </button>
          <button className={`center-btn ${step === 'CAMERAS' ? 'active-step' : ''}`} disabled={!analysis?.valid || (step !== 'COLOR' && step !== 'CAMERAS')} onClick={() => setStep('CAMERAS')}>
            4. Place Cameras
          </button>
          <button className="center-btn" style={{ marginTop: '20px' }} disabled={!isClosed} onClick={handleResetPipeline}>
            Reset Pipeline
          </button>

          {step === 'SWEEP_LINE' && analysis?.valid && (
            <div className="card" style={{ marginTop: '20px' }}>
              <h3 style={{ fontSize: '0.75rem' }}>Vertex Classification</h3>
              <p className="sweep-progress">
                {sweepCompleted
                  ? `Classification complete · ${classifiedVertexCount} vertices`
                  : `Sweeping top to bottom · ${classifiedVertexCount}/${vertices.length} classified`}
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: VERTEX_TYPE_COLORS.START }}>▲</span> Start Vertex
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: VERTEX_TYPE_COLORS.END }}>▼</span> End Vertex
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: VERTEX_TYPE_COLORS.SPLIT, fontSize: '1.2rem' }}>♦</span> Split Vertex
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: VERTEX_TYPE_COLORS.MERGE }}>■</span> Merge Vertex
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: VERTEX_TYPE_COLORS.REGULAR }}>●</span> Regular Vertex
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}