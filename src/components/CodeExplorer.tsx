import React, { useState } from 'react';
import { Download, Copy, Check, FileCode, Folder, ShieldCheck } from 'lucide-react';
import JSZip from 'jszip';
import { ANDROID_PROJECT_FILES, AndroidProjectFile } from '../data/androidFiles';

export const CodeExplorer: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<AndroidProjectFile>(ANDROID_PROJECT_FILES[0]);
  const [copied, setCopied] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  const handleCopyCode = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadZip = async () => {
    setIsExporting(true);
    try {
      const zip = new JSZip();
      const root = zip.folder('AimShot_Project');

      if (root) {
        // Add all project files into the zip archive
        ANDROID_PROJECT_FILES.forEach((file) => {
          root.file(file.path, file.content);
        });

        // Add root build.gradle and settings.gradle
        root.file(
          'build.gradle',
          `// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.2.2'
    }
}
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}`
        );

        root.file(
          'settings.gradle',
          `rootProject.name = "AimShot Engine"\ninclude ':app'`
        );

        root.file(
          'gradle.properties',
          `android.useAndroidX=true\nandroid.enableJetifier=true`
        );

        const content = await zip.generateAsync({ type: 'blob' });
        const url = URL.createObjectURL(content);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'AimShot_Engine_Android_Project.zip';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      }
    } catch (err) {
      console.error('Failed to generate zip', err);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="w-full h-full flex flex-col bg-[#0B0F17] border border-[#1C2B42] rounded-2xl overflow-hidden shadow-2xl">
      {/* Top Header Bar */}
      <div className="h-14 px-5 bg-[#111B2A] border-b border-[#1C2B42] flex items-center justify-between">
        <div className="flex items-center gap-3">
          <FileCode className="w-5 h-5 text-[#39B9FF]" />
          <div className="flex flex-col">
            <span className="text-sm font-bold text-white tracking-wide">
              Native Android Project (Java + XML)
            </span>
            <span className="text-[11px] text-[#8D9BB0] font-mono">
              Package: com.alpha10.aimshot
            </span>
          </div>
        </div>

        <button
          onClick={handleDownloadZip}
          disabled={isExporting}
          className="px-4 py-2 rounded-xl bg-[#39B9FF] hover:bg-[#2da5e6] text-[#0B0F17] font-bold text-xs flex items-center gap-2 transition-all shadow-[0_0_12px_rgba(57,185,255,0.3)] cursor-pointer disabled:opacity-50"
        >
          <Download className="w-4 h-4" />
          <span>{isExporting ? 'Packaging ZIP...' : 'Download Android Studio Project (.ZIP)'}</span>
        </button>
      </div>

      {/* Main Split Body: File Tree & Code View */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Sidebar: File Tree */}
        <div className="w-72 bg-[#090D14] border-r border-[#1C2B42] flex flex-col p-3 overflow-y-auto shrink-0 select-none">
          <div className="text-[11px] font-bold uppercase tracking-wider text-[#55657E] px-2 py-1 mb-1">
            Project Architecture
          </div>

          <div className="space-y-1">
            {ANDROID_PROJECT_FILES.map((file) => (
              <button
                key={file.path}
                onClick={() => setSelectedFile(file)}
                className={`w-full text-left px-3 py-2 rounded-xl text-xs font-mono transition-all flex items-center gap-2.5 cursor-pointer ${
                  selectedFile.path === file.path
                    ? 'bg-[#14283D] text-[#39B9FF] font-bold border border-[#39B9FF]/40'
                    : 'text-[#8D9BB0] hover:bg-[#111B2A] hover:text-white'
                }`}
              >
                <Folder className="w-3.5 h-3.5 text-[#39B9FF]/70 shrink-0" />
                <span className="truncate">{file.name}</span>
              </button>
            ))}
          </div>

          <div className="mt-auto pt-4 border-t border-[#1C2B42]/50 px-2 text-[11px] text-[#8D9BB0]">
            <div className="flex items-center gap-1.5 text-[#49DCA0] font-medium mb-1">
              <ShieldCheck className="w-4 h-4" />
              <span>Strict Rules Enforced</span>
            </div>
            <p className="text-[10px] text-[#55657E]">
              • Java + Native XML exclusively<br />
              • No Kotlin dependencies<br />
              • Authentic com.alpha10.aimshot package
            </p>
          </div>
        </div>

        {/* Right Area: Code Display */}
        <div className="flex-1 flex flex-col bg-[#080C12] overflow-hidden">
          {/* File Tab Bar */}
          <div className="h-10 px-4 bg-[#0D1420] border-b border-[#1C2B42] flex items-center justify-between select-none">
            <span className="text-xs font-mono text-[#39B9FF]">
              {selectedFile.path}
            </span>

            <button
              onClick={handleCopyCode}
              className="flex items-center gap-1.5 text-xs text-[#8D9BB0] hover:text-[#39B9FF] transition-colors cursor-pointer"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-[#49DCA0]" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'Copied' : 'Copy File'}</span>
            </button>
          </div>

          {/* Description banner */}
          <div className="px-4 py-2 bg-[#111B2A]/40 border-b border-[#1C2B42]/50 text-[11px] text-[#8D9BB0]">
            {selectedFile.description}
          </div>

          {/* Code Body */}
          <div className="flex-1 p-4 overflow-auto">
            <pre className="font-mono text-xs text-[#E2E8F0] leading-relaxed selection:bg-[#39B9FF]/30">
              <code>{selectedFile.content}</code>
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};
