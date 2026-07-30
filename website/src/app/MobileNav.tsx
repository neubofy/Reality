'use client';
import React, { useState } from 'react';
import Link from 'next/link';
import { Menu, X, Crown } from 'lucide-react';

export default function MobileNav() {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <div className="sm:hidden">
      <button 
        onClick={() => setIsOpen(!isOpen)} 
        className="text-gray-300 hover:text-white p-2 rounded-md focus:outline-none"
        aria-label="Toggle navigation menu"
      >
        {isOpen ? <X size={24} /> : <Menu size={24} />}
      </button>
      {isOpen && (
        <div className="absolute top-16 left-0 w-full bg-neural-card border-b border-gray-800 p-4 flex flex-col gap-4 font-mono text-sm shadow-xl z-50">
          <Link onClick={() => setIsOpen(false)} href="/" className="text-gray-300 hover:text-neural-cyan px-3 py-2 rounded-md">HOME</Link>
          <Link onClick={() => setIsOpen(false)} href="/tapashya" className="text-gray-300 hover:text-neural-cyan px-3 py-2 rounded-md">TAPASYA WEB</Link>
          <Link onClick={() => setIsOpen(false)} href="/promembers" className="text-yellow-500 hover:text-yellow-400 px-3 py-2 rounded-md flex items-center gap-2">
            <Crown size={14}/> PRO MEMBERS
          </Link>
          <Link onClick={() => setIsOpen(false)} href="/privacypolicy" className="text-gray-400 hover:text-white px-3 py-2 rounded-md">PRIVACY</Link>
        </div>
      )}
    </div>
  )
}
