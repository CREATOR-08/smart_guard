import React, { useState, useRef, useEffect } from 'react';

const API_URL = 'https://smart-guard-hpze.onrender.com/api/polygon/analyze';

const PRESETS = {
  simple: [
    { x: 150, y: 90 }, { x: 520, y: 90 }, { x: 610, y: 240 }, { x: 480, y: 380 }, { x: 180, y: 380 }
  ],
  lShape: [
    { x: 120, y: 80 }, { x: 360, y: 80 }, { x: 360, y: 240 }, { x: 580, y: 240 }, { x: 580, y: 390 }, { x: 120, y: 390 }
  ],
  complex: [
    { x: 90, y: 90 }, { x: 300, y: 70 }, { x: 340, y: 190 }, { x: 470, y: 80 }, { x: 650, y: 120 },
    { x: 620, y: 390 }, { x: 430, y: 300 }, { x: 320, y: 400 }, { x: 190, y: 290 }, { x: 90, y: 380 }
  ],
  convex: [
    { x: 260, y: 70 }, { x: 460, y: 70 }, { x: 600, y: 170 }, { x: 600, y: 310 }, { x: 460, y: 400 },
    { x: 260, y: 400 }, { x: 130, y: 310 }, { x: 130, y: 170 }
  ],
  maze: [
    { x: 50, y: 50 }, { x: 730, y: 50 }, { x: 730, y: 120 }, { x: 120, y: 120 },
    { x: 120, y: 190 }, { x: 730, y: 190 }, { x: 730, y: 260 }, { x: 120, y: 260 },
    { x: 120, y: 330 }, { x: 730, y: 330 }, { x: 730, y: 400 }, { x: 50, y: 400 }
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

const COLOR_HEX = ['#ef4444', '#22c55e', '#3b82f6'];
const COLOR_NAMES = ['Color 1 (Red)', 'Color 2 (Green)', 'Color 3 (Blue)'];

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

  useEffect(() => {
    if (step === 'TRIANGULATE' && analysis?.triangles) {
      setVisibleTrianglesCount(0);
      let current = 0;
      const total = analysis.triangles.length;
      const timer = setInterval(() => {
        current += 1;
        setVisibleTrianglesCount(current);
        if (current >= total) clearInterval(timer);
      }, 350);
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

        if (step === 'TRIANGULATE' && i === count - 1) {
          ctx.fillStyle = 'rgba(250, 204, 21, 0.28)';
        } else {
          ctx.fillStyle = 'rgba(148, 163, 184, 0.08)';
        }
        ctx.fill();

        ctx.strokeStyle = '#f59e0b';
        ctx.setLineDash([6, 4]);
        ctx.lineWidth = 1.5;
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
      ctx.strokeStyle = analysis?.valid === false ? '#ef4444' : '#38bdf8';
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

    const activeCameras = step === 'CAMERAS' && analysis?.valid
      ? (cameraMode === 'GREEDY' ? analysis.greedySetCoverCameras : analysis.artGalleryCameras)
      : [];

    const showSweepLineShapes = analysis?.valid && (step === 'FINISHED' || step === 'TRIANGULATE');

    vertices.forEach((v, idx) => {
      const isCamera = activeCameras.includes(idx);
      const vType = analysis?.vertexTypes ? analysis.vertexTypes[idx] : 'REGULAR';
      
      ctx.beginPath();

      // Draw specialized geometric shapes during the sweep-line/triangulation phases
      if (showSweepLineShapes && !isCamera) {
        if (vType === 'START') {
          ctx.moveTo(v.x, v.y - 9); ctx.lineTo(v.x - 8, v.y + 7); ctx.lineTo(v.x + 8, v.y + 7);
          ctx.fillStyle = '#3b82f6'; // Blue Triangle Up
        } else if (vType === 'END') {
          ctx.moveTo(v.x, v.y + 9); ctx.lineTo(v.x - 8, v.y - 7); ctx.lineTo(v.x + 8, v.y - 7);
          ctx.fillStyle = '#22c55e'; // Green Triangle Down
        } else if (vType === 'SPLIT') {
          ctx.moveTo(v.x, v.y - 10); ctx.lineTo(v.x - 8, v.y); ctx.lineTo(v.x, v.y + 10); ctx.lineTo(v.x + 8, v.y);
          ctx.fillStyle = '#ef4444'; // Red Diamond
        } else if (vType === 'MERGE') {
          ctx.rect(v.x - 7, v.y - 7, 14, 14);
          ctx.fillStyle = '#f97316'; // Orange Square
        } else {
          ctx.arc(v.x, v.y, 7, 0, Math.PI * 2);
          ctx.fillStyle = '#f8fafc'; // White Circle (Regular)
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
  }, [vertices, isClosed, mousePos, step, analysis, visibleTrianglesCount, cameraMode]);

  const handleCanvasClick = (e) => {
    if (isClosed) return;
    const rect = canvasRef.current.getBoundingClientRect();
    const x = Math.round(e.clientX - rect.left);
    const y = Math.round(e.clientY - rect.top);
    setVertices((prev) => [...prev, { x, y }]);
  };

  const handleMouseMove = (e) => {
    const rect = canvasRef.current.getBoundingClientRect();
    setMousePos({
      x: Math.round(e.clientX - rect.left),
      y: Math.round(e.clientY - rect.top)
    });
  };

  const runBackendAnalysis = async (pts) => {
    setErrorMsg(null);
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
    setVertices(pts);
    runBackendAnalysis(pts);
  };

  const handleClear = () => {
    setVertices([]);
    setIsClosed(false);
    setAnalysis(null);
    setStep('DRAW');
    setErrorMsg(null);
  };

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
          <h3 className="section-title">Example Problems</h3>
          <button onClick={() => loadPreset('simple')}>Simple Polygon</button>
          <button onClick={() => loadPreset('lShape')}>L-Shape</button>
          <button onClick={() => loadPreset('complex')}>Complex</button>
          <button onClick={() => loadPreset('convex')}>Convex</button>
          <button onClick={() => loadPreset('maze')}>Maze</button>
          <button onClick={() => loadPreset('comb')}>Worst-Case Comb</button>
          <button onClick={() => loadPreset('star')}>Star</button>
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
                      <span style={{ 
                        fontSize: '0.75rem', 
                        color: '#f8fafc', 
                        background: analysis?.vertexTypes?.[i] === 'START' ? '#3b82f6' :
                                   analysis?.vertexTypes?.[i] === 'END' ? '#22c55e' :
                                   analysis?.vertexTypes?.[i] === 'SPLIT' ? '#ef4444' :
                                   analysis?.vertexTypes?.[i] === 'MERGE' ? '#f97316' : '#475569',
                        padding: '2px 6px', 
                        borderRadius: '4px',
                        fontWeight: 'bold'
                      }}>
                        {analysis?.vertexTypes ? analysis.vertexTypes[i] : 'REGULAR'}
                      </span>
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
          <button className={`center-btn ${step === 'TRIANGULATE' ? 'active-step' : ''}`} disabled={!analysis?.valid} onClick={() => setStep('TRIANGULATE')}>
            1. Triangulate
          </button>
          <button className={`center-btn ${step === 'COLOR' ? 'active-step' : ''}`} disabled={!analysis?.valid || (step !== 'TRIANGULATE' && step !== 'COLOR' && step !== 'CAMERAS')} onClick={() => setStep('COLOR')}>
            2. 3-Color Graph
          </button>
          <button className={`center-btn ${step === 'CAMERAS' ? 'active-step' : ''}`} disabled={!analysis?.valid || (step !== 'COLOR' && step !== 'CAMERAS')} onClick={() => setStep('CAMERAS')}>
            3. Place Cameras
          </button>
          <button className="center-btn" style={{ marginTop: '20px' }} disabled={!isClosed} onClick={() => setStep('FINISHED')}>
            Reset Pipeline
          </button>

          {/* Sweep-Line Legend (Only visible before coloring) */}
          {(step === 'DRAW' || step === 'FINISHED' || step === 'TRIANGULATE') && analysis?.valid && (
            <div className="card" style={{ marginTop: '20px' }}>
              <h3 style={{ fontSize: '0.75rem' }}>Sweep-Line Legend</h3>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: '#3b82f6' }}>▲</span> Start Vertex
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: '#22c55e' }}>▼</span> End Vertex
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: '#ef4444', fontSize: '1.2rem' }}>♦</span> Split Vertex
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: '#f97316' }}>■</span> Merge Vertex
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', margin: '4px 0' }}>
                <span style={{ color: '#f8fafc' }}>●</span> Regular Vertex
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}