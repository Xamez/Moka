declare global {
  interface Window {
    invoke: (address: string, payload: any) => Promise<any>
  }
}

export const invoke = async <T = any>(address: string, payload: any = {}): Promise<T> => {
  if (typeof window === 'undefined') {
    console.log("client side only")
    return Promise.reject('Invoke is client-side only')
  }

  if (typeof window.invoke !== 'undefined') {
    console.log("invoked")
    return window.invoke(address, payload)
  }

  console.groupCollapsed('Calling Moka address:', address)

  try {
    const response = await fetch('http://localhost:8080/_moka/bridge', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ address, payload }),
    })

    if (response.ok) {
      const data = await response.json()
      console.debug('Backend response:', data)
      console.groupEnd()
      return data as T
    }
  } catch (e) {
    console.debug('Fetch error:', e)
  }

  console.groupEnd()

  return Promise.reject('Moka backend unreachable')
}
