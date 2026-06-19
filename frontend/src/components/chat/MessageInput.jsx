import { useState, useRef, useEffect, useCallback } from 'react';
import { Send, Paperclip } from 'lucide-react';

export default function MessageInput({ onSend, onAttachment, onTyping, disabled, conversationId }) {
  const [text, setText] = useState('');
  const textareaRef = useRef(null);
  const fileRef = useRef(null);
  const typingTimeoutRef = useRef(null);

  const publishTyping = useCallback((typing) => {
    if (onTyping && conversationId) onTyping(typing);
  }, [onTyping, conversationId]);

  const handleTextChange = (value) => {
    setText(value);
    publishTyping(true);
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    typingTimeoutRef.current = setTimeout(() => publishTyping(false), 2000);
  };

  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setText('');
    publishTyping(false);
    if (textareaRef.current) textareaRef.current.style.height = 'auto';
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleFile = (e) => {
    const file = e.target.files?.[0];
    if (file && onAttachment) onAttachment(file);
    e.target.value = '';
  };

  useEffect(() => {
    const el = textareaRef.current;
    if (el) {
      el.style.height = 'auto';
      el.style.height = Math.min(el.scrollHeight, 120) + 'px';
    }
  }, [text]);

  useEffect(() => () => {
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
  }, []);

  return (
    <div className="message-input-container">
      <div className="message-input-wrapper">
        <button
          type="button"
          className="btn-icon"
          style={{ flexShrink: 0 }}
          onClick={() => fileRef.current?.click()}
          disabled={disabled}
          title="Attach file"
        >
          <Paperclip size={18} />
        </button>
        <input ref={fileRef} type="file" accept="image/*,.pdf,.txt,.doc,.docx" hidden onChange={handleFile} />
        <textarea
          ref={textareaRef}
          className="message-textarea"
          placeholder="Type a message..."
          value={text}
          onChange={(e) => handleTextChange(e.target.value)}
          onKeyDown={handleKeyDown}
          rows={1}
          disabled={disabled}
        />
        <button
          className="send-btn"
          onClick={handleSend}
          disabled={!text.trim() || disabled}
          title="Send message"
        >
          <Send size={18} />
        </button>
      </div>
    </div>
  );
}
