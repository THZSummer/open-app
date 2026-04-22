import { BrowserRouter } from 'react-router-dom';
import Router from './router';

function App() {
  return (
    <BrowserRouter basename="/open-web">
      <Router />
    </BrowserRouter>
  );
}

export default App;
