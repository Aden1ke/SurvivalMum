import { memo } from 'react';

const ActionButton = memo(({ children, variant = 'primary', size = 'md', onClick, disabled, fullWidth, id }) => {
  const cls = [
    'btn',
    `btn-${variant}`,
    size === 'lg' ? 'btn-lg' : '',
    fullWidth ? 'btn-full' : '',
  ].join(' ');

  return (
    <button id={id} className={cls} onClick={onClick} disabled={disabled}
      style={{ opacity: disabled ? 0.5 : 1, cursor: disabled ? 'not-allowed' : 'pointer' }}>
      {children}
    </button>
  );
});

export default ActionButton;
