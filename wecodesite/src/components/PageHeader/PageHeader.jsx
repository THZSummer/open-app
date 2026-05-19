import React from 'react';
import { Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import './PageHeader.less';

function PageHeader({
  title,
  description,
  buttonText,
  onButtonClick,
  linkText,
  linkUrl,
}) {
  const navigate = useNavigate();

  const handleLinkClick = (e) => {
    e.preventDefault();
    if (linkUrl) {
      navigate(linkUrl);
    }
  };

  return (
    <div className="page-header">
      <div className="page-header-left">
        {title && <h4 className="page-title">{title}</h4>}
        {description && (
          <span className="page-desc">
            {description}
            {linkText && linkUrl && (
              <a onClick={handleLinkClick}>{linkText}</a>
            )}
          </span>
        )}
      </div>
      {buttonText && (
        <Button
          type="primary"
          onClick={onButtonClick}
          style={{ justifyContent: 'center', borderRadius: 6 }}
        >
          {buttonText}
        </Button>
      )}
    </div>
  );
}

export default PageHeader;
